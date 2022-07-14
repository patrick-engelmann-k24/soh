package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.clients.PricingServiceClient;
import de.kfzteile24.salesOrderHub.clients.ProductDataHubClient;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.domain.pdh.product.ProductSet;
import de.kfzteile24.salesOrderHub.dto.pricing.Prices;
import de.kfzteile24.salesOrderHub.dto.pricing.PricingItem;
import de.kfzteile24.salesOrderHub.dto.pricing.SetUnitPriceAPIResponse;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.UnitValues;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getProductEnvelope;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemSplitServiceTest {

    @Spy
    @InjectMocks
    private ItemSplitService itemSplitService;

    @Mock
    private ProductDataHubClient productDataHubClient;

    @Mock
    private PricingServiceClient pricingServiceClient;

    @Mock
    private OrderUtil orderUtil;

    @Mock
    private FeatureFlagConfig featureFlagConfig;

    @Test
    void processOrderList() {

        final var setSku = "2270-13013";
        final var fakeProductJson = "DZN.json";

        final var firstSetItemSku = "1410-4610";
        final var secondSetItemSku = "1420-4355";
        // "real" set item
        getProductFromJson(setSku, "2270-13013.json");

        // other products in the order (no sets; no need to replace them)
        getProductFromJson("2270-13012", fakeProductJson);
        getProductFromJson("2270-13015", fakeProductJson);
        // replacement products
        getProductFromJson(firstSetItemSku, fakeProductJson);
        getProductFromJson(secondSetItemSku, fakeProductJson);

        final var order1 = getOrder(readResource("examples/splitterSalesOrderMessageWithTwoRows.json"));
        final var order2 = getOrder(readResource("examples/splitterSalesOrderMessageWithTwoRows.json"));
        final var list = new ArrayList<Order>();
        list.add(order1);
        list.add(order2);

        when(featureFlagConfig.getPreventSetProcessing()).thenReturn(false);
        when(pricingServiceClient.getSetPriceInfo(any(), any(), any())).thenReturn(Optional.of(SetUnitPriceAPIResponse.builder().setUnitPrices(List.of(PricingItem.builder().build())).build()));
        doNothing().when(itemSplitService).recalculateSumValuesForSetItemPrices(any(), any(), any(), any(), any());
        doNothing().when(itemSplitService).recalculateUnitValuesForSetItems(any(), any(), any());
        doNothing().when(itemSplitService).flattenDifference(any(), any(), any(), any());
        doNothing().when(itemSplitService).recalculateSumValues(any(), any(), any(), any(), any());
        doNothing().when(itemSplitService).checkSumValuesDifference(any(), any(), any(), any());
        when(orderUtil.getLastRowKey(any(Order.class))).thenReturn(3);

        itemSplitService.processOrder(order1);
        itemSplitService.processOrder(order2);

        for (Order order : list) {
            final var rows = order.getOrderRows();

            // check if setItem is NOT in the list
            assertThat(getCountForSku(rows, setSku)).isEqualTo(0);

            assertThat(getCountForSku(rows, firstSetItemSku)).isEqualTo(1);
            final var firstReplacementItem = findRowBySku(rows, firstSetItemSku);
            assertThat(firstReplacementItem).isNotNull();
            assertThat(firstReplacementItem.getQuantity()).isEqualTo(BigDecimal.valueOf(1));
            assertThat(firstReplacementItem.getRowKey()).isEqualTo(4);

            assertThat(getCountForSku(rows, secondSetItemSku)).isEqualTo(1);
            final var secondReplacementItem = findRowBySku(rows, secondSetItemSku);
            assertThat(secondReplacementItem).isNotNull();
            assertThat(secondReplacementItem.getQuantity()).isEqualTo(BigDecimal.valueOf(2));
            assertThat(secondReplacementItem.getRowKey()).isEqualTo(5);
        }
    }

    @Test
    void processOrder() {

        final var setSku = "2270-13013";
        final var fakeProductJson = "DZN.json";

        final var firstSetItemSku = "1410-4610";
        final var secondSetItemSku = "1420-4355";
        // "real" set item
        getProductFromJson(setSku, "2270-13013.json");

        // other products in the order (no sets; no need to replace them)
        getProductFromJson("2270-13012", fakeProductJson);
        getProductFromJson("2270-13015", fakeProductJson);
        // replacement products
        getProductFromJson(firstSetItemSku, fakeProductJson);
        getProductFromJson(secondSetItemSku, fakeProductJson);

        final var order = getOrder(readResource("examples/splitterSalesOrderMessageWithTwoRows.json"));

        when(featureFlagConfig.getPreventSetProcessing()).thenReturn(false);
        when(pricingServiceClient.getSetPriceInfo(any(), any(), any())).thenReturn(Optional.of(SetUnitPriceAPIResponse.builder().setUnitPrices(List.of(PricingItem.builder().build())).build()));
        doNothing().when(itemSplitService).recalculateSumValuesForSetItemPrices(any(), any(), any(), any(), any());
        doNothing().when(itemSplitService).recalculateUnitValuesForSetItems(any(), any(), any());
        doNothing().when(itemSplitService).flattenDifference(any(), any(), any(), any());
        doNothing().when(itemSplitService).recalculateSumValues(any(), any(), any(), any(), any());
        doNothing().when(itemSplitService).checkSumValuesDifference(any(), any(), any(), any());
        when(orderUtil.getLastRowKey(any(Order.class))).thenReturn(3);
        itemSplitService.processOrder(order);

        final var rows = order.getOrderRows();

        // check if setItem is NOT in the list
        assertThat(getCountForSku(rows, setSku)).isEqualTo(0);

        assertThat(getCountForSku(rows, firstSetItemSku)).isEqualTo(1);
        final var firstReplacementItem = findRowBySku(rows, firstSetItemSku);
        assertThat(firstReplacementItem).isNotNull();
        assertThat(firstReplacementItem.getQuantity()).isEqualTo(BigDecimal.valueOf(1));
        assertThat(firstReplacementItem.getRowKey()).isEqualTo(4);

        assertThat(getCountForSku(rows, secondSetItemSku)).isEqualTo(1);
        final var secondReplacementItem = findRowBySku(rows, secondSetItemSku);
        assertThat(secondReplacementItem).isNotNull();
        assertThat(secondReplacementItem.getQuantity()).isEqualTo(BigDecimal.valueOf(2));
        assertThat(secondReplacementItem.getRowKey()).isEqualTo(5);

    }

    @Test
    void processOrderWhenPDHIsNotAvailable() {

        final var order = getOrder(readResource("examples/splitterSalesOrderMessageWithTwoRows.json"));
        assertThatThrownBy(() -> itemSplitService.processOrder(order))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProduct() {

        final var productEnvelope = getProductEnvelope(readResource("examples/product/2270-13013.json"));
        when(productDataHubClient.getProductBySku("2270-13013")).thenReturn(productEnvelope.getProduct());

        final var product = itemSplitService.getProduct("2270-13013");
        assertThat(product).isEqualTo(productEnvelope.getProduct());
    }

    @Test
    void mapProductToOrderRows() {

        final var product = getProductEnvelope(readResource("examples/product/2270-13013.json"));
        final var order = getOrder(readResource("examples/splitterSalesOrderMessageWithTwoRows.json"));
        final var firstRow = order.getOrderRows().get(0);
        final var locale = order.getOrderHeader().getLocale();
        firstRow.setCustomerNote("foobar");

        final OrderRows rows = itemSplitService.mapProductToOrderRows(product.getProduct(), firstRow, BigDecimal.valueOf(2), locale);

        assertThat(rows).isNotNull();

        assertThat(rows.getIsCancelled()).isEqualTo(false);
        assertThat(rows.getIsPriceHammer()).isEqualTo(false);
        assertThat(rows.getSku()).isEqualTo(firstRow.getSku());
        assertThat(rows.getEan()).isEqualTo("4250032492922");
        assertThat(rows.getGenart()).isEqualTo("816");
        assertThat(rows.getSetReferenceId()).isEqualTo(firstRow.getSku());
        assertThat(rows.getSetReferenceName()).isEqualTo(firstRow.getName());
        assertThat(rows.getCustomerNote()).isEqualTo(firstRow.getCustomerNote());
        assertThat(rows.getQuantity()).isEqualTo(BigDecimal.valueOf(2));
        assertThat(rows.getTaxRate()).isEqualTo(firstRow.getTaxRate());
        assertThat(rows.getPartIdentificationProperties()).isEqualTo(firstRow.getPartIdentificationProperties());
    }

    @Test
    public void testGetLocaleString() {

        String locale = "de_DE";
        String otherLocale = "DE";
        assertEquals("DE", itemSplitService.getLocaleString(locale));
        assertEquals("DE", itemSplitService.getLocaleString(otherLocale));
    }

    @Test
    void testRecalculateSetItemPrices() {

        OrderRows orderRow1 = createEmptyOrderRows("sku-1");
        OrderRows orderRow2 = createEmptyOrderRows("sku-2");
        orderRow1.setQuantity(new BigDecimal("1.0"));
        orderRow2.setQuantity(new BigDecimal("3.0"));
        List<OrderRows> setItems = List.of(orderRow1, orderRow2);

        UnitValues setUnitValues = UnitValues.builder()
                .goodsValueGross(new BigDecimal("20.00")).goodsValueNet(new BigDecimal("18.00"))
                .discountedGross(new BigDecimal("20.00")).discountedNet(new BigDecimal("18.00")).build();

        PricingItem pricingItem1 = createPricingItem(
                new BigDecimal("3.50"), new BigDecimal("3.00"), new BigDecimal("0.25"), "sku-1");
        PricingItem pricingItem2 = createPricingItem(
                new BigDecimal("13.50"), new BigDecimal("13.00"), new BigDecimal("0.75"), "sku-2");
        List<PricingItem> pricingItems = List.of(pricingItem1, pricingItem2);

        itemSplitService.recalculateSumValuesForSetItemPrices(setItems, setUnitValues, pricingItems, "orderNumber", "sku");
        itemSplitService.recalculateUnitValuesForSetItems(setItems, "orderNumber", "sku");

        assertEquals(new BigDecimal("5.00"), orderRow1.getUnitValues().getGoodsValueGross());
        assertEquals(new BigDecimal("4.50"), orderRow1.getUnitValues().getGoodsValueNet());
        assertEquals(new BigDecimal("5.00"), orderRow1.getUnitValues().getDiscountedGross());
        assertEquals(new BigDecimal("4.50"), orderRow1.getUnitValues().getDiscountedNet());
        assertEquals(new BigDecimal("5.00"), orderRow1.getSumValues().getGoodsValueGross());
        assertEquals(new BigDecimal("4.50"), orderRow1.getSumValues().getGoodsValueNet());
        assertEquals(new BigDecimal("5.00"), orderRow1.getSumValues().getTotalDiscountedGross());
        assertEquals(new BigDecimal("4.50"), orderRow1.getSumValues().getTotalDiscountedNet());

        assertEquals(new BigDecimal("5.00"), orderRow2.getUnitValues().getGoodsValueGross());
        assertEquals(new BigDecimal("4.50"), orderRow2.getUnitValues().getGoodsValueNet());
        assertEquals(new BigDecimal("5.00"), orderRow2.getUnitValues().getDiscountedGross());
        assertEquals(new BigDecimal("4.50"), orderRow2.getUnitValues().getDiscountedNet());
        assertEquals(new BigDecimal("15.00"), orderRow2.getSumValues().getGoodsValueGross());
        assertEquals(new BigDecimal("13.50"), orderRow2.getSumValues().getGoodsValueNet());
        assertEquals(new BigDecimal("15.00"), orderRow2.getSumValues().getTotalDiscountedGross());
        assertEquals(new BigDecimal("13.50"), orderRow2.getSumValues().getTotalDiscountedNet());
    }

    @Test
    void testRecalculateSetItemPricesAddDifference() {

        BigDecimal oneCent = new BigDecimal("0.01");

        OrderRows orderRow1 = createEmptyOrderRows("sku-1");
        OrderRows orderRow2 = createEmptyOrderRows("sku-2");
        OrderRows orderRow3 = createEmptyOrderRows("sku-3");
        orderRow1.setQuantity(new BigDecimal("1.0"));
        orderRow2.setQuantity(new BigDecimal("1.0"));
        orderRow3.setQuantity(new BigDecimal("1.0"));
        List<OrderRows> setItems = List.of(orderRow1, orderRow2, orderRow3);

        UnitValues setUnitValues = UnitValues.builder()
                .goodsValueGross(new BigDecimal("2.92")).goodsValueNet(new BigDecimal("1.92"))
                .discountedGross(new BigDecimal("2.92")).discountedNet(new BigDecimal("1.92")).build();

        PricingItem pricingItem1 = createPricingItem(
                new BigDecimal("3.50"), new BigDecimal("3.00"), new BigDecimal("0.34"), "sku-1");
        PricingItem pricingItem2 = createPricingItem(
                new BigDecimal("13.50"), new BigDecimal("13.00"), new BigDecimal("0.33"), "sku-2");
        PricingItem pricingItem3 = createPricingItem(
                new BigDecimal("42.19"), new BigDecimal("41.20"), new BigDecimal("0.33"), "sku-3");
        List<PricingItem> pricingItems = List.of(pricingItem1, pricingItem2, pricingItem3);

        itemSplitService.recalculateSumValuesForSetItemPrices(setItems, setUnitValues, pricingItems, "orderNumber", "sku");
        itemSplitService.recalculateUnitValuesForSetItems(setItems, "orderNumber", "sku");
        itemSplitService.flattenDifference(setItems, setUnitValues, "orderNumber", "sku");

        assertEquals(new BigDecimal("0.99"), orderRow1.getUnitValues().getGoodsValueGross());
        assertEquals(new BigDecimal("0.65"), orderRow1.getUnitValues().getGoodsValueNet());
        assertEquals(new BigDecimal("0.99"), orderRow1.getUnitValues().getDiscountedGross());
        assertEquals(new BigDecimal("0.65"), orderRow1.getUnitValues().getDiscountedNet());
        assertEquals(new BigDecimal("0.99").add(oneCent), orderRow1.getSumValues().getGoodsValueGross());
        assertEquals(new BigDecimal("0.65").add(oneCent), orderRow1.getSumValues().getGoodsValueNet());
        assertEquals(new BigDecimal("0.99").add(oneCent), orderRow1.getSumValues().getTotalDiscountedGross());
        assertEquals(new BigDecimal("0.65").add(oneCent), orderRow1.getSumValues().getTotalDiscountedNet());

        assertEquals(new BigDecimal("0.96"), orderRow2.getUnitValues().getGoodsValueGross());
        assertEquals(new BigDecimal("0.63"), orderRow2.getUnitValues().getGoodsValueNet());
        assertEquals(new BigDecimal("0.96"), orderRow2.getUnitValues().getDiscountedGross());
        assertEquals(new BigDecimal("0.63"), orderRow2.getUnitValues().getDiscountedNet());
        assertEquals(new BigDecimal("0.96"), orderRow2.getSumValues().getGoodsValueGross());
        assertEquals(new BigDecimal("0.63"), orderRow2.getSumValues().getGoodsValueNet());
        assertEquals(new BigDecimal("0.96"), orderRow2.getSumValues().getTotalDiscountedGross());
        assertEquals(new BigDecimal("0.63"), orderRow2.getSumValues().getTotalDiscountedNet());

        assertEquals(new BigDecimal("0.96"), orderRow3.getUnitValues().getGoodsValueGross());
        assertEquals(new BigDecimal("0.63"), orderRow3.getUnitValues().getGoodsValueNet());
        assertEquals(new BigDecimal("0.96"), orderRow3.getUnitValues().getDiscountedGross());
        assertEquals(new BigDecimal("0.63"), orderRow3.getUnitValues().getDiscountedNet());
        assertEquals(new BigDecimal("0.96"), orderRow3.getSumValues().getGoodsValueGross());
        assertEquals(new BigDecimal("0.63"), orderRow3.getSumValues().getGoodsValueNet());
        assertEquals(new BigDecimal("0.96"), orderRow3.getSumValues().getTotalDiscountedGross());
        assertEquals(new BigDecimal("0.63"), orderRow3.getSumValues().getTotalDiscountedNet());
    }

    @Test
    void testRecalculateSetItemPricesNotMatched() {

        OrderRows orderRow1 = createEmptyOrderRows("sku-1");
        OrderRows orderRow2 = createEmptyOrderRows("sku-2");
        orderRow1.setQuantity(new BigDecimal("1.0"));
        orderRow2.setQuantity(new BigDecimal("2.0"));
        List<OrderRows> setItems = List.of(orderRow1, orderRow2);

        UnitValues setUnitValues = UnitValues.builder()
                .goodsValueGross(new BigDecimal("20.00")).goodsValueNet(new BigDecimal("18.00"))
                .discountedGross(new BigDecimal("20.00")).discountedNet(new BigDecimal("18.00")).build();

        assertThatThrownBy(() -> itemSplitService.flattenDifference(setItems, setUnitValues, "orderNumber", "sku"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discounted Gross prices from Pricing Service do not add up.Set cannot be split.");
    }

    @Test
    void testFallbackCalculationForValueShare() {

        ProductSet productSet1 = new ProductSet();
        productSet1.setQuantity(new BigDecimal("1.0"));
        productSet1.setSku("sku-1");
        ProductSet productSet2 = new ProductSet();
        productSet2.setQuantity(new BigDecimal("1.0"));
        productSet2.setSku("sku-2");
        ProductSet productSet3 = new ProductSet();
        productSet3.setQuantity(new BigDecimal("1.0"));
        productSet3.setSku("sku-3");
        List<ProductSet> setItems = List.of(productSet1, productSet2, productSet3);

        when(pricingServiceClient.getSetPriceInfo(any(), any(), any())).thenReturn(Optional.empty());

        List<PricingItem> setPrices = itemSplitService.getSetPrices("set-sku", "salesChannel", "orderNumber", setItems);
        assertEquals(3, setPrices.size());

        PricingItem pricingItem1 = setPrices.stream().filter(item -> item.getSku().equals("sku-1")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.33333333333333333"), pricingItem1.getValueShare());

        PricingItem pricingItem2 = setPrices.stream().filter(item -> item.getSku().equals("sku-2")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.33333333333333333"), pricingItem2.getValueShare());

        PricingItem pricingItem3 = setPrices.stream().filter(item -> item.getSku().equals("sku-3")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.33333333333333333"), pricingItem3.getValueShare());
    }

    @Test
    void testFallbackCalculationForIncompletePrices() {

        ProductSet productSet1 = new ProductSet();
        productSet1.setQuantity(new BigDecimal("1.0"));
        productSet1.setSku("sku-1");
        ProductSet productSet2 = new ProductSet();
        productSet2.setQuantity(new BigDecimal("1.0"));
        productSet2.setSku("sku-2");
        ProductSet productSet3 = new ProductSet();
        productSet3.setQuantity(new BigDecimal("1.0"));
        productSet3.setSku("sku-3");
        List<ProductSet> setItems = List.of(productSet1, productSet2, productSet3);

        Optional<SetUnitPriceAPIResponse> setUnitPrices = Optional.of(SetUnitPriceAPIResponse.builder()
                .setUnitPrices(List.of(PricingItem.builder().build()))
                .build());
        when(pricingServiceClient.getSetPriceInfo(any(), any(), any())).thenReturn(setUnitPrices);

        List<PricingItem> setPrices = itemSplitService.getSetPrices("set-sku", "salesChannel", "orderNumber", setItems);
        assertEquals(3, setPrices.size());

        PricingItem pricingItem1 = setPrices.stream().filter(item -> item.getSku().equals("sku-1")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.33333333333333333"), pricingItem1.getValueShare());

        PricingItem pricingItem2 = setPrices.stream().filter(item -> item.getSku().equals("sku-2")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.33333333333333333"), pricingItem2.getValueShare());

        PricingItem pricingItem3 = setPrices.stream().filter(item -> item.getSku().equals("sku-3")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.33333333333333333"), pricingItem3.getValueShare());
    }

    private OrderRows createEmptyOrderRows(String sku) {

        return OrderRows.builder()
                .sku(sku)
                .unitValues(UnitValues.builder().build())
                .sumValues(SumValues.builder().build())
                .build();
    }

    private PricingItem createPricingItem(BigDecimal unitGross, BigDecimal unitNet, BigDecimal valueShare, String sku) {

        return PricingItem.builder()
                .unitPrices(Prices.builder().gross(unitGross).net(unitNet).build())
                .valueShare(valueShare)
                .sku(sku)
                .build();
    }

    protected void getProductFromJson(final String sku, final String json) {
        final var replacementProductEnvelope = getProductEnvelope(readResource("examples/product/".concat(json)));
        final var replacementProduct = replacementProductEnvelope.getProduct();
        replacementProduct.setSku(sku);

        when(productDataHubClient.getProductBySku(sku)).thenReturn(replacementProduct);
    }

    protected int getCountForSku(List<OrderRows> rows, final String sku) {
        return (int) rows.stream().filter(r -> Objects.equals(r.getSku(), sku)).count();
    }

    protected OrderRows findRowBySku(List<OrderRows> rows, final String sku) {
        return rows.stream().filter(r -> Objects.equals(r.getSku(), sku)).findFirst().orElseThrow();
    }

}
