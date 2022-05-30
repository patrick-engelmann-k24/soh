package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.clients.ProductDataHubClient;
import de.kfzteile24.salesOrderHub.domain.pdh.Product;
import de.kfzteile24.salesOrderHub.domain.pdh.product.Country;
import de.kfzteile24.salesOrderHub.domain.pdh.product.Localization;
import de.kfzteile24.salesOrderHub.domain.pricing.PricingItem;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.round;

/**
 * This class splits up set items into single items
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemSplitService extends AbstractSplitDecorator {

    @Autowired
    private ProductDataHubClient productDataHubClient;

    @Autowired
    private OrderUtil orderUtil;

    @Override
    public void processOrderList(ArrayList<Order> orderList) {

        for (final var order : orderList) {
            processOrder(order);
        }
    }

    public void processOrder(Order order) {

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
                final var setItems = new ArrayList<OrderRows>();
                List<PricingItem> itemPrices = getSetPrices(row.getSku());
                for (final var setItem : product.getSetProductCollection()) {
                    final BigDecimal qty = setItem.getQuantity().multiply(row.getQuantity());
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
                recalculateSetItemPrices(setItems, row.getSumValues(), itemPrices);
                replacementProductCollection.addAll(setItems);
                originItemsWhichGetReplaced.add(row);
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
     * @param setSku - sku of the set product
     * @return get list of the prices for the items in the set
     */
    protected List<PricingItem> getSetPrices(String setSku) {

        return List.of();
    }

    /**
     * map the PDH product to a OrderRows-Object
     *
     * @param product - the PDH product
     * @return get a single OrderRows-object
     */
    protected OrderRows mapProductToOrderRows(
            final Product product, final OrderRows originItem, BigDecimal quantity, String locale) {

        final var localeStr = getLocaleString(locale);
        Country country = product.getCountries().getOrDefault(localeStr, Country.builder().build());
        Localization localization = product.getLocalizations().getOrDefault(localeStr, Localization.builder().build());
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
                .unitValues(originItem.getUnitValues())
                .sumValues(originItem.getSumValues())
                .manufacturerProductNumber(productNumber)
                .build();
    }

    /**
     * @param setItems     - the items in the origin set
     * @param setSumValues - the set sum values
     * @param prices       - the list of prices for the items in the set
     */
    protected void recalculateSetItemPrices(
            List<OrderRows> setItems, final SumValues setSumValues, List<PricingItem> prices) {

        for (OrderRows orderRow : setItems) {
            UnitValues unitValues = orderRow.getUnitValues();
            SumValues sumValues = orderRow.getSumValues();
            PricingItem pricingItem = prices.stream().filter(Objects::nonNull)
                    .filter(price -> price.getSku().equals(orderRow.getSku())).findFirst().orElseThrow();
            BigDecimal unitGross =
                    round(Optional.ofNullable(pricingItem.getUnitPrices().getGross()).orElse(BigDecimal.ZERO));
            BigDecimal unitNet =
                    round(Optional.ofNullable(pricingItem.getUnitPrices().getNet()).orElse(BigDecimal.ZERO));
            BigDecimal sumGross = round(Optional.of(pricingItem.getValueShare()).orElse(BigDecimal.ZERO)
                    .multiply(setSumValues.getGoodsValueGross()));
            BigDecimal sumNet = round(Optional.of(pricingItem.getValueShare()).orElse(BigDecimal.ZERO)
                    .multiply(setSumValues.getGoodsValueNet()));

            unitValues.setGoodsValueGross(unitGross);
            unitValues.setGoodsValueNet(unitNet);
            unitValues.setDiscountedGross(unitGross);
            unitValues.setDiscountedNet(unitNet);

            sumValues.setGoodsValueGross(sumGross);
            sumValues.setGoodsValueNet(sumNet);
            sumValues.setTotalDiscountedGross(sumGross);
            sumValues.setTotalDiscountedNet(sumNet);
        }

        BigDecimal totalSum = getSumValue(SumValues::getGoodsValueGross,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        BigDecimal difference = setSumValues.getGoodsValueGross().subtract(totalSum);

        if (difference.abs().compareTo(new BigDecimal("0.01")) == 0) {
            BigDecimal newSumGrossPrice = setItems.get(0).getSumValues().getGoodsValueGross().add(difference);
            setItems.get(0).getSumValues().setGoodsValueGross(newSumGrossPrice);
            setItems.get(0).getSumValues().setTotalDiscountedGross(newSumGrossPrice);
        } else if (difference.compareTo(BigDecimal.ZERO) != 0) {
            String errorMessage = "Prices from Pricing Service do not add up. Set cannot be split.";
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        totalSum = getSumValue(SumValues::getGoodsValueNet,
                setItems.stream().map(OrderRows::getSumValues).collect(Collectors.toList()));
        difference = setSumValues.getGoodsValueNet().subtract(totalSum);

        if (difference.abs().compareTo(new BigDecimal("0.01")) == 0) {
            BigDecimal newSumNetPrice = setItems.get(0).getSumValues().getGoodsValueNet().add(difference);
            setItems.get(0).getSumValues().setGoodsValueNet(newSumNetPrice);
            setItems.get(0).getSumValues().setTotalDiscountedNet(newSumNetPrice);
        } else if (difference.compareTo(BigDecimal.ZERO) != 0) {
            String errorMessage = "Prices from Pricing Service do not add up. Set cannot be split.";
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    protected String getLocaleString(String locale) {

        String[] split = locale.split("_");
        return split[split.length - 1];
    }

}
