package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.clients.ProductDataHubClient;
import de.kfzteile24.salesOrderHub.domain.pdh.Product;
import de.kfzteile24.salesOrderHub.domain.pdh.product.Country;
import de.kfzteile24.salesOrderHub.domain.pdh.product.Localization;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;

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
                final var setReplacementProductCollection = new ArrayList<OrderRows>();
                for (final var includedProductPDH : product.getSetProductCollection()) {
                    final BigDecimal qty = includedProductPDH.getQuantity();
                    final var pdhProduct = getProduct(includedProductPDH.getSku());
                    if (pdhProduct != null) {
                        final var replacementProduct = mapProductToOrderRows(pdhProduct, row, qty, locale);
                        replacementProduct.setRowKey(rowKey);
                        setReplacementProductCollection.add(replacementProduct);
                        ++rowKey;
                    } else {
                        log.error("Could not get product data from PDH for sku: {}", includedProductPDH.getSku());
                        throw new NotFoundException("Could not get product data from PDH for sku: " + includedProductPDH.getSku());
                    }
                }
                recalculatePrices(setReplacementProductCollection, row);
                replacementProductCollection.addAll(setReplacementProductCollection);
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
     * map the PDH product to a OrderRows-Object
     *
     * @param product - the PDH product
     * @return get a single OrderRows-object
     */
    protected OrderRows mapProductToOrderRows(
            final Product product, final OrderRows originItem, BigDecimal quantity, String locale) {

        final var localeStr = locale.substring(0, locale.indexOf('_')).toUpperCase();
        Country country = product.getCountries().getOrDefault(localeStr, Country.builder().build());
        Localization localization = product.getLocalizations().getOrDefault(localeStr, Localization.builder().build());
        String genart = null;
        String ean = null;
        String productNumber = null;
        if (country.getGenart() != null) {
            genart = country.getGenart().size() > 0 ? country.getGenart().get(0) : null;
        }
        if (country.getEan() != null) {
            ean = country.getEan().size() > 0 ? country.getEan().get(0) : null;
        }
        if (product.getPartNumbers() != null) {
            productNumber = product.getPartNumbers().size() > 0 ? product.getPartNumbers().get(0).getPartNumber() : null;
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
     * @param replacementCollection - the items in the origin set
     * @param row                   - the set itself
     */
    protected void recalculatePrices(ArrayList<OrderRows> replacementCollection, final OrderRows row) {
        // todo recalculate prices here
        // newProduct.setUnitValues();
        // newProduct.setSumValues();
    }

}
