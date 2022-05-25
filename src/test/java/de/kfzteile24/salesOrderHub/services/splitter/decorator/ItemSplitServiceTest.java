package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.clients.ProductDataHubClient;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getProductEnvelope;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemSplitServiceTest {

    @InjectMocks
    private ItemSplitService itemSplitService;

    @Mock
    private ProductDataHubClient productDataHubClient;

    @Mock
    private OrderUtil orderUtil;

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

        when(orderUtil.getLastRowKey(any(Order.class))).thenReturn(3);

        itemSplitService.processOrderList(list);

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
