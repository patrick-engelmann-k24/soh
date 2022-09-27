package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.clients.PricingServiceClient;
import de.kfzteile24.salesOrderHub.clients.ProductDataHubClient;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.domain.pdh.Product;
import de.kfzteile24.salesOrderHub.domain.pdh.product.Country;
import de.kfzteile24.salesOrderHub.domain.pdh.product.Localization;
import de.kfzteile24.salesOrderHub.domain.pdh.product.ProductSet;
import de.kfzteile24.salesOrderHub.dto.pricing.PricingItem;
import de.kfzteile24.salesOrderHub.dto.pricing.SetUnitPriceAPIResponse;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.UnitValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.TWO_CENTS;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.round;

/**
 * This class splits up set items into single items
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemSplitService {

    @Autowired
    private ProductDataHubClient productDataHubClient;

    @Autowired
    private PricingServiceClient pricingServiceClient;

    @Autowired
    private OrderUtil orderUtil;

    @Autowired
    private FeatureFlagConfig featureFlagConfig;

    public void processOrder(Order order) {

        final var orderNumber = order.getOrderHeader().getOrderNumber();
        final var salesChannel = order.getOrderHeader().getSalesChannel();
        final var locale = order.getOrderHeader().getLocale();
        var rowKey = orderUtil.getLastRowKey(order) + 1;
        final var originItemsWhichGetReplaced = new ArrayList<OrderRows>();
        final var replacementProductCollection = new ArrayList<OrderRows>();
        for (final var row : order.getOrderRows()) {
            final var product = getProduct(row.getSku());
            if (product == null) {
                log.error("Could not get product data from PDH for sku: {}", row.getSku());
                throw new NotFoundException("Could not get product data from PDH for sku: " + row.getSku());
            } else if (product.isSetItem()) {
                if (featureFlagConfig.getPreventSetProcessing()) {
                    log.error("Order number {} contains a setitem and cannot be processed", orderNumber);
                    throw new IllegalArgumentException("Order number " + orderNumber + " contains a setitem and cannot be processed");
                } else {
                    final var setItems = new ArrayList<OrderRows>();
                    List<PricingItem> itemPrices = getSetPrices(row.getSku(), salesChannel, orderNumber, product.getSetProductCollection());
                    for (final var setItem : product.getSetProductCollection()) {
                        final BigDecimal qty = setItem.getQuantity();
                        final var pdhProduct = getProduct(setItem.getSku());
                        if (pdhProduct != null) {
                            final var replacementProduct = mapProductToOrderRows(pdhProduct, row, qty, locale);
                            replacementProduct.setRowKey(rowKey);
                            setItems.add(replacementProduct);
                            ++rowKey;
                        } else {
                            String errorMessage = "Could not get product data from PDH for sku: " + setItem.getSku();
                            log.error(errorMessage);
                            throw new NotFoundException(errorMessage);
                        }
                    }
                    recalculateSumValuesForSetItemPrices(setItems, row.getUnitValues(), itemPrices, orderNumber, row.getSku());
                    recalculateUnitValuesForSetItems(setItems, orderNumber, row.getSku());
                    flattenDifference(setItems, row.getUnitValues(), orderNumber, row.getSku());
                    recalculateSumValues(setItems, row.getSumValues(), row.getQuantity(), orderNumber, row.getSku());
                    checkSumValuesDifference(setItems, row.getSumValues(), orderNumber, row.getSku());

                    replacementProductCollection.addAll(setItems);
                    originItemsWhichGetReplaced.add(row);
                }
            }
        }
        order.getOrderRows().removeAll(originItemsWhichGetReplaced);
        order.getOrderRows().addAll(replacementProductCollection);
    }

    protected Product getProduct(String sku) {

        return productDataHubClient.getProductBySku(sku);
    }

    /**
     * call pricing service to get prices for the items in the set
     *
     * @param setSku           - sku of the set product
     * @param salesChannelCode - sales channel for which to get the prices
     * @param orderNumber      - order number
     * @param setProducts      - list of product data from PDH for fallback calculation
     * @return get list of the prices for the items in the set
     */
    protected List<PricingItem> getSetPrices(
            String setSku, String salesChannelCode, String orderNumber, List<ProductSet> setProducts) {

        Optional<SetUnitPriceAPIResponse> setPriceInfo =
                pricingServiceClient.getSetPriceInfo(setSku, salesChannelCode, orderNumber);
        if (setPriceInfo.isPresent()) {
            List<PricingItem> setUnitPrices = setPriceInfo.get().getSetUnitPrices();
            List<String> setItemSkus = setUnitPrices.stream().map(PricingItem::getSku).collect(Collectors.toList());
            for (ProductSet productSet : setProducts) {
                if (!setItemSkus.contains(productSet.getSku())) {
                    log.info("Pricing system does not have the set item prices for order number {} and sku {}, " +
                            "missing sku is {} " +
                            "calculating the prices based on quantity", orderNumber, setSku, productSet.getSku());
                    return fallbackSetItemCalculation(setProducts);
                }
            }
            return setUnitPrices;
        } else {
            log.info("Pricing system does not have the set item prices for order number {} and sku {}, " +
                    "calculating the prices based on quantity", orderNumber, setSku);
            return fallbackSetItemCalculation(setProducts);
        }
    }

    private List<PricingItem> fallbackSetItemCalculation(List<ProductSet> setProducts) {

        final var quantitySum = setProducts.stream()
                .map(ProductSet::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        return setProducts.stream().map(sp ->
                        PricingItem.builder()
                                .valueShare(sp.getQuantity().divide(quantitySum, 17, RoundingMode.HALF_UP))
                                .sku(sp.getSku())
                                .build())
                .collect(Collectors.toList());
    }

    /**
     * map the PDH product to a OrderRows-Object
     *
     * @param product - the PDH product
     * @return get a single OrderRows-object
     */
    protected OrderRows mapProductToOrderRows(
            final Product product, final OrderRows originItem, BigDecimal quantity, String locale) {

        final var languageCode = getLanguageCode(locale);
        final var countryCode = getCountryCode(locale);
        Country country = product.getCountries().getOrDefault(countryCode, Country.builder().build());
        Localization localization = product.getLocalizations().getOrDefault(languageCode, Localization.builder().build());
        String genart = null;
        String ean = null;
        String productNumber = null;
        if (country.getGenart() != null) {
            genart = country.getGenart().size() > 0 ? country.getGenart().get(0) : null;
        } else {
            log.info("Could not get genart from PDH for sku: {}", product.getSku());
        }
        if (country.getEan() != null) {
            ean = country.getEan().size() > 0 ? country.getEan().get(0) : null;
        } else {
            log.info("Could not get ean from PDH for sku: {}", product.getSku());
        }
        if (product.getPartNumbers() != null) {
            if (product.getPartNumbers().size() > 0) {
                productNumber = product.getPartNumbers().get(0).getPartNumber();
            } else {
                log.info("Could not get product number  from PDH for sku: {}", product.getSku());
            }
        } else {
            log.info("Could not get product number  from PDH for sku: {}", product.getSku());
        }

        return OrderRows.builder()
                .isCancelled(originItem.getIsCancelled())
                .isPriceHammer(originItem.getIsPriceHammer())
                .sku(product.getSku())
                .name(localization.getName())
                .ean(ean)
                .genart(genart)
                .setReferenceId(originItem.getSku())
                .setReferenceName(originItem.getName())
                .customerNote(originItem.getCustomerNote())
                .quantity(quantity)
                .taxRate(originItem.getTaxRate())
                .partIdentificationProperties(originItem.getPartIdentificationProperties())
                .estimatedDeliveryDate(originItem.getEstimatedDeliveryDate())
                .shippingType(originItem.getShippingType())
                .clickCollectBranchId(originItem.getClickCollectBranchId())
                .shippingAddressKey(originItem.getShippingAddressKey())
                .shippingProvider(originItem.getShippingProvider())
                .trackingNumbers(originItem.getTrackingNumbers())
                .unitValues(UnitValues.builder().build())
                .sumValues(SumValues.builder().build())
                .manufacturerProductNumber(productNumber)
                .build();
    }

    /**
     * @param setItems      - the items in the origin set
     * @param setUnitValues - the set sum values
     * @param prices        - the list of prices for the items in the set
     */
    protected void recalculateSumValuesForSetItemPrices(
            List<OrderRows> setItems, final UnitValues setUnitValues, List<PricingItem> prices,
            String orderNumber, String setItemSku) {

        log.info("Recalculating Sum Values for set items for order number: {} and sku: {}", orderNumber, setItemSku);

        for (OrderRows orderRow : setItems) {
            log.info("calculating sum values for set item with sku: {}, initial unit values: {} orderNumber: {}",
                    orderRow.getSku(), setUnitValues, orderNumber);
            SumValues sumValues = orderRow.getSumValues();
            PricingItem pricingItem = prices.stream().filter(Objects::nonNull)
                    .filter(price -> price.getSku().equals(orderRow.getSku())).findFirst().orElseThrow();

            BigDecimal sumGoodsValueGross = round(Optional.of(pricingItem.getValueShare()).orElse(BigDecimal.ZERO)
                    .multiply(setUnitValues.getGoodsValueGross()));
            BigDecimal sumGoodsValueNet = round(Optional.of(pricingItem.getValueShare()).orElse(BigDecimal.ZERO)
                    .multiply(setUnitValues.getGoodsValueNet()));
            BigDecimal sumTotalDiscountedGross = round(Optional.of(pricingItem.getValueShare()).orElse(BigDecimal.ZERO)
                    .multiply(setUnitValues.getDiscountedGross()));
            BigDecimal sumTotalDiscountedNet = round(Optional.of(pricingItem.getValueShare()).orElse(BigDecimal.ZERO)
                    .multiply(setUnitValues.getDiscountedNet()));

            sumValues.setGoodsValueGross(sumGoodsValueGross);
            sumValues.setGoodsValueNet(sumGoodsValueNet);
            sumValues.setTotalDiscountedGross(sumTotalDiscountedGross);
            sumValues.setTotalDiscountedNet(sumTotalDiscountedNet);
            sumValues.setDiscountGross(sumGoodsValueGross.subtract(sumTotalDiscountedGross));
            sumValues.setDiscountNet(sumGoodsValueNet.subtract(sumTotalDiscountedNet));

            log.info("SumValues initially calculated for sku: {}, sum values: {}, order number {}",
                    orderRow.getSku(), sumValues, orderNumber);
        }
    }

    protected void recalculateUnitValuesForSetItems(List<OrderRows> setItems, String orderNumber, String setItemSku) {

        log.info("Recalculating unit Values for set items for order number: {} and sku: {}", orderNumber, setItemSku);

        for (OrderRows orderRow : setItems) {
            log.info("calculating unit values for set item with sku: {}, sum values: {}, quantity: {}, orderNumber: {}",
                    orderRow.getSku(), orderRow.getSumValues(), orderRow.getQuantity(), orderNumber);

            SumValues sumValues = orderRow.getSumValues();
            UnitValues unitValues = orderRow.getUnitValues();

            BigDecimal unitGoodsValueGross = round(sumValues.getGoodsValueGross().divide(orderRow.getQuantity(), RoundingMode.HALF_UP));
            BigDecimal unitGoodsValueNet = round(sumValues.getGoodsValueNet().divide(orderRow.getQuantity(), RoundingMode.HALF_UP));
            BigDecimal unitDiscountedGross = round(sumValues.getTotalDiscountedGross().divide(orderRow.getQuantity(), RoundingMode.HALF_UP));
            BigDecimal unitDiscountedNet = round(sumValues.getTotalDiscountedNet().divide(orderRow.getQuantity(), RoundingMode.HALF_UP));
            unitValues.setGoodsValueGross(unitGoodsValueGross);
            unitValues.setGoodsValueNet(unitGoodsValueNet);
            unitValues.setDiscountedGross(unitDiscountedGross);
            unitValues.setDiscountedNet(unitDiscountedNet);
            unitValues.setDiscountGross(unitGoodsValueGross.subtract(unitDiscountedGross));
            unitValues.setDiscountNet(unitGoodsValueNet.subtract(unitDiscountedNet));
        }
    }

    protected void flattenDifference(List<OrderRows> setItems, UnitValues setUnitValues, String orderNumber, String setItemSku) {

        BigDecimal totalDiscountedGrossSum = getSumValue(SumValues::getTotalDiscountedGross,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        BigDecimal totalDiscountGrossDifference = setUnitValues.getDiscountedGross().subtract(totalDiscountedGrossSum);

        /*
         * if the totalDiscountGrossDifference between the set price and the sum of the set items is less than 2 cents we add it to the first item
         * if the totalDiscountGrossDifference is greater we throw an exception
         */
        if (totalDiscountGrossDifference.abs().compareTo(TWO_CENTS) <= 0) {
            SumValues firstSetItemSumValues = setItems.get(0).getSumValues();
            BigDecimal newGrossPrice = firstSetItemSumValues.getTotalDiscountedGross().add(totalDiscountGrossDifference);
            firstSetItemSumValues.setTotalDiscountedGross(newGrossPrice);
        } else if (totalDiscountGrossDifference.compareTo(BigDecimal.ZERO) != 0) {
            log.error("Discounted Gross prices from Pricing Service do not add up for order number: {} and sku: {}.\n" +
                            "Initial set gross price: {}, set gross price from pricing: {}\n" +
                            "Set cannot be split.",
                    orderNumber, setItemSku, setUnitValues.getDiscountedGross(), totalDiscountedGrossSum);
            throw new IllegalArgumentException("Discounted Gross prices from Pricing Service do not add up.Set cannot be split.");
        }

        BigDecimal totalDiscountedNetSum = getSumValue(SumValues::getTotalDiscountedNet,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        BigDecimal totalDiscountNetDifference = setUnitValues.getDiscountedNet().subtract(totalDiscountedNetSum);

        /*
         * if the totalDiscountNetDifference between the set price and the sum of the set items is less than 2 cents we add it to the first item
         * if the totalDiscountNetDifference is greater we throw an exception
         */
        if (totalDiscountNetDifference.abs().compareTo(TWO_CENTS) <= 0) {
            SumValues firstSetItemSumValues = setItems.get(0).getSumValues();
            BigDecimal newNetPrice = firstSetItemSumValues.getTotalDiscountedNet().add(totalDiscountNetDifference);
            firstSetItemSumValues.setTotalDiscountedNet(newNetPrice);
        } else if (totalDiscountNetDifference.compareTo(BigDecimal.ZERO) != 0) {
            log.error("Discounted Net prices from Pricing Service do not add up for order number: {} and sku: {}.\n" +
                            "Initial set net price: {}, set net price from pricing: {}\n" +
                            "Set cannot be split.",
                    orderNumber, setItemSku, setUnitValues.getDiscountedNet(), totalDiscountedNetSum);
            throw new IllegalArgumentException("Discounted Net prices from Pricing Service do not add up.Set cannot be split.");
        }

        BigDecimal goodsValueGrossSum = getSumValue(SumValues::getGoodsValueGross,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        BigDecimal goodsValueGrossDifference = setUnitValues.getGoodsValueGross().subtract(goodsValueGrossSum);

        /*
         * if the goodsValueGrossDifference between the set price and the sum of the set items is less than 2 cents we add it to the first item
         * if the goodsValueGrossDifference is greater we throw an exception
         */
        if (goodsValueGrossDifference.abs().compareTo(TWO_CENTS) <= 0) {
            SumValues firstSetItemSumValues = setItems.get(0).getSumValues();
            BigDecimal newGrossPrice = firstSetItemSumValues.getGoodsValueGross().add(goodsValueGrossDifference);
            firstSetItemSumValues.setGoodsValueGross(newGrossPrice);
        } else if (goodsValueGrossDifference.compareTo(BigDecimal.ZERO) != 0) {
            log.error("Goods Value Gross prices from Pricing Service do not add up for order number: {} and sku: {}.\n" +
                            "Initial set gross price: {}, set gross price from pricing: {}\n" +
                            "Set cannot be split.",
                    orderNumber, setItemSku, setUnitValues.getGoodsValueGross(), goodsValueGrossSum);
            throw new IllegalArgumentException("Goods Value Gross prices from Pricing Service do not add up.Set cannot be split.");
        }

        BigDecimal goodsValueNetSum = getSumValue(SumValues::getGoodsValueNet,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        BigDecimal goodsValueNetDifference = setUnitValues.getGoodsValueNet().subtract(goodsValueNetSum);

        /*
         * if the goodsValueNetDifference between the set price and the sum of the set items is less than 2 cents we add it to the first item
         * if the goodsValueNetDifference is greater we throw an exception
         */
        if (goodsValueNetDifference.abs().compareTo(TWO_CENTS) <= 0) {
            SumValues firstSetItemSumValues = setItems.get(0).getSumValues();
            BigDecimal newNetPrice = firstSetItemSumValues.getGoodsValueNet().add(goodsValueNetDifference);
            firstSetItemSumValues.setGoodsValueNet(newNetPrice);
        } else if (goodsValueNetDifference.compareTo(BigDecimal.ZERO) != 0) {
            log.error("Goods Value Net prices from Pricing Service do not add up for order number: {} and sku: {}.\n" +
                            "Initial set net price: {}, set net price from pricing: {}\n" +
                            "Set cannot be split.",
                    orderNumber, setItemSku, setUnitValues.getGoodsValueNet(), goodsValueNetSum);
            throw new IllegalArgumentException("Goods Value Net prices from Pricing Service do not add up.Set cannot be split.");
        }
    }

    protected void recalculateSumValues(List<OrderRows> setItems, SumValues setSumValues, BigDecimal setQuantity,
                                        String orderNumber, String setItemSku) {

        for (OrderRows orderRow : setItems) {
            log.info("Calculating sum prices for set item with sku: {}, initial sum gross: {} initial sum net: {}\n" +
                            "orderNumber: {}, set sku: {}",
                    orderRow.getSku(), setSumValues.getGoodsValueGross(), setSumValues.getGoodsValueNet(),
                    orderNumber, setItemSku);

            SumValues sumValues = orderRow.getSumValues();
            BigDecimal sumGoodsValueGross = round(sumValues.getGoodsValueGross().multiply(setQuantity));
            BigDecimal sumGoodsValueNet = round(sumValues.getGoodsValueNet().multiply(setQuantity));
            BigDecimal sumDiscountedGross = round(sumValues.getTotalDiscountedGross().multiply(setQuantity));
            BigDecimal sumDiscountedNet = round(sumValues.getTotalDiscountedNet().multiply(setQuantity));

            sumValues.setGoodsValueGross(sumGoodsValueGross);
            sumValues.setGoodsValueNet(sumGoodsValueNet);
            sumValues.setTotalDiscountedGross(sumDiscountedGross);
            sumValues.setTotalDiscountedNet(sumDiscountedNet);
            log.info("SumValues finally calculated for sku: {}, sum values: {}\n" +
                    "orderNumber: {}, set sku: {}", orderRow.getSku(), sumValues, orderNumber, setItemSku);
            orderRow.setQuantity(orderRow.getQuantity().multiply(setQuantity));
        }
    }

    protected void checkSumValuesDifference(List<OrderRows> setItems, SumValues setSumValues, String orderNumber, String setItemSku) {

        BigDecimal totalDiscountedGrossSum = getSumValue(SumValues::getTotalDiscountedGross,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        BigDecimal totalDiscountGrossDifference = setSumValues.getTotalDiscountedGross().subtract(totalDiscountedGrossSum);

        /*
         * if the totalDiscountGrossDifference is less than or equal to 2 cents we add the difference to the highest sum values of the set items
         * if the totalDiscountGrossDifference is not 0 we throw an exception
         */
        if (totalDiscountGrossDifference.abs().compareTo(TWO_CENTS) <= 0) {
            OrderRows highestSetItems = setItems.stream()
                    .max(Comparator.comparing(row -> row.getSumValues().getTotalDiscountedGross()))
                    .orElseThrow(NoSuchElementException::new);
            BigDecimal newGrossPrice = highestSetItems.getSumValues().getTotalDiscountedGross().add(totalDiscountGrossDifference);
            highestSetItems.getSumValues().setTotalDiscountedGross(newGrossPrice);
        } else if (totalDiscountGrossDifference.abs().compareTo(BigDecimal.ZERO) != 0) {
            log.error("Discounted Gross prices from Pricing Service do not add up for order number: {} and sku: {}.\n" +
                            "Initial set gross price: {}, set gross price from pricing: {}\n" +
                            "Set cannot be split.",
                    orderNumber, setItemSku, setSumValues.getTotalDiscountedGross(), totalDiscountedGrossSum);
            throw new IllegalArgumentException("Discounted Gross prices from Pricing Service do not add up.Set cannot be split.");
        }

        BigDecimal totalDiscountedNetSum = getSumValue(SumValues::getTotalDiscountedNet,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        BigDecimal totalDiscountNetDifference = setSumValues.getTotalDiscountedNet().subtract(totalDiscountedNetSum);

        /*
         * if the totalDiscountNetDifference is less than or equal to 2 cents we add the difference to the highest sum values of the set items
         * if the totalDiscountNetDifference is not 0 greater we throw an exception
         */
        if (totalDiscountNetDifference.abs().compareTo(TWO_CENTS) <= 0) {
            OrderRows highestSetItems = setItems.stream()
                    .max(Comparator.comparing(row -> row.getSumValues().getTotalDiscountedNet()))
                    .orElseThrow(NoSuchElementException::new);
            BigDecimal newNetPrice = highestSetItems.getSumValues().getTotalDiscountedNet().add(totalDiscountNetDifference);
            highestSetItems.getSumValues().setTotalDiscountedNet(newNetPrice);
        } else if (totalDiscountNetDifference.compareTo(BigDecimal.ZERO) != 0) {
            log.error("Discounted Net prices from Pricing Service do not add up for order number: {} and sku: {}.\n" +
                            "Initial set net price: {}, set net price from pricing: {}\n" +
                            "Set cannot be split.",
                    orderNumber, setItemSku, setSumValues.getTotalDiscountedNet(), totalDiscountedNetSum);
            throw new IllegalArgumentException("Discounted Net prices from Pricing Service do not add up.Set cannot be split.");
        }

        BigDecimal goodsValueGrossSum = getSumValue(SumValues::getGoodsValueGross,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        BigDecimal goodsValueGrossDifference = setSumValues.getGoodsValueGross().subtract(goodsValueGrossSum);

        /*
         * if the goodsValueGrossDifference is less than or equal to 2 cents we add the difference to the highest sum values of the set items
         * if the goodsValueGrossDifference is not 0 we throw an exception
         */
        if (goodsValueGrossDifference.abs().compareTo(TWO_CENTS) <= 0) {
            OrderRows highestSetItems = setItems.stream()
                    .max(Comparator.comparing(row -> row.getSumValues().getGoodsValueGross()))
                    .orElseThrow(NoSuchElementException::new);
            BigDecimal newGrossPrice = highestSetItems.getSumValues().getGoodsValueGross().add(goodsValueGrossDifference);
            highestSetItems.getSumValues().setGoodsValueGross(newGrossPrice);
        } else if (goodsValueGrossDifference.compareTo(BigDecimal.ZERO) != 0) {
            log.error("Goods Value Gross prices from Pricing Service do not add up for order number: {} and sku: {}.\n" +
                            "Initial set gross price: {}, set gross price from pricing: {}\n" +
                            "Set cannot be split.",
                    orderNumber, setItemSku, setSumValues.getGoodsValueGross(), goodsValueGrossSum);
            throw new IllegalArgumentException("Goods Value Gross prices from Pricing Service do not add up.Set cannot be split.");
        }

        BigDecimal goodsValueNetSum = getSumValue(SumValues::getGoodsValueNet,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        BigDecimal goodsValueNetDifference = setSumValues.getGoodsValueNet().subtract(goodsValueNetSum);

        /*
         * if the goodsValueNetDifference is less than or equal to 2 cents we add the difference to the highest sum values of the set items
         * if the goodsValueNetDifference is not 0 we throw an exception
         */
        if (goodsValueNetDifference.abs().compareTo(TWO_CENTS) <= 0) {
            OrderRows highestSetItems = setItems.stream()
                    .max(Comparator.comparing(row -> row.getSumValues().getGoodsValueNet()))
                    .orElseThrow(NoSuchElementException::new);
            BigDecimal newNetPrice = highestSetItems.getSumValues().getGoodsValueNet().add(goodsValueNetDifference);
            highestSetItems.getSumValues().setGoodsValueNet(newNetPrice);
        } else if (goodsValueNetDifference.compareTo(BigDecimal.ZERO) != 0) {
            log.error("Goods Value Net prices from Pricing Service do not add up for order number: {} and sku: {}.\n" +
                            "Initial set net price: {}, set net price from pricing: {}\n" +
                            "Set cannot be split.",
                    orderNumber, setItemSku, setSumValues.getGoodsValueNet(), goodsValueNetSum);
            throw new IllegalArgumentException("Goods Value Net prices from Pricing Service do not add up.Set cannot be split.");
        }
    }

    protected String getCountryCode(String locale) {
        String[] split = locale.split("_");
        return split[split.length - 1];
    }

    protected String getLanguageCode(String locale) {
        String[] split = locale.split("_");
        return split[0].toUpperCase();
    }

}
