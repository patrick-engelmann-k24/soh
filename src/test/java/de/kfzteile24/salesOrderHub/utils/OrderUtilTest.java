package de.kfzteile24.salesOrderHub.utils;

import de.kfzteile24.salesOrderHub.configuration.DropShipmentConfig;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderHeader;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.UnitValues;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.OrderUtil.getOrderGroupIdFromOrderNumber;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.soh.order.dto.Platform.BRAINCRAFT;
import static de.kfzteile24.soh.order.dto.Platform.CORE;
import static de.kfzteile24.soh.order.dto.Platform.ECP;
import static de.kfzteile24.soh.order.dto.Platform.EMIDA;
import static de.kfzteile24.soh.order.dto.Platform.SOH;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUtilTest {



    @Mock
    private DropShipmentConfig dropShipmentConfig;

    @InjectMocks
    private OrderUtil orderUtil;

    @Test
    void testGetLastRowKey() {
        final var salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class));

        final var lastRowKey = orderUtil.getLastRowKey(salesOrder);

        assertThat(lastRowKey).isEqualTo(2);
    }

    @Test
    void testIsDropshipmentItem() {

        when(dropShipmentConfig.getEcp()).thenReturn(List.of("10033", "10034", "10035"));
        when(dropShipmentConfig.getDeshop()).thenReturn(List.of("10040", "10041", "10042"));

        assertTrue(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10033").build(), ECP));
        assertTrue(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10034").build(), ECP));
        assertTrue(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10035").build(), ECP));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10036").build(), ECP));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10040").build(), ECP));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10041").build(), ECP));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10042").build(), ECP));

        assertTrue(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10040").build(), BRAINCRAFT));
        assertTrue(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10041").build(), BRAINCRAFT));
        assertTrue(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10042").build(), BRAINCRAFT));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10043").build(), BRAINCRAFT));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10033").build(), BRAINCRAFT));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10034").build(), BRAINCRAFT));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10035").build(), BRAINCRAFT));

        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10033").build(), CORE));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10034").build(), CORE));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10035").build(), CORE));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10040").build(), CORE));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10041").build(), CORE));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10042").build(), CORE));

        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10033").build(), EMIDA));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10034").build(), EMIDA));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10035").build(), EMIDA));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10040").build(), EMIDA));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10041").build(), EMIDA));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10042").build(), EMIDA));

        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10033").build(), SOH));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10034").build(), SOH));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10035").build(), SOH));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10040").build(), SOH));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10041").build(), SOH));
        assertFalse(orderUtil.isDropShipmentItem(OrderRows.builder().genart("10042").build(), SOH));


    }

    @Test
    void testCreateShippingCostLineFromSalesOrder() {
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber("invoice-number");
        for (OrderRows row : salesOrder.getLatestJson().getOrderRows()) {
            row.setUnitValues(UnitValues.builder()
                    .goodsValueGross(BigDecimal.valueOf(9))
                    .goodsValueNet(BigDecimal.valueOf(3))
                    .discountGross(BigDecimal.valueOf(3))
                    .discountNet(BigDecimal.valueOf(1))
                    .discountedGross(BigDecimal.valueOf(6))
                    .discountedNet(BigDecimal.valueOf(2))
                    .build());
        }

        assertThat(orderUtil.hasShippingCost(salesOrder)).isTrue();

        var shippingCostLine = orderUtil.createShippingCostLineFromSalesOrder(salesOrder);
        assertThat(shippingCostLine.getIsShippingCost()).isTrue();
        assertThat(shippingCostLine.getItemNumber()).isEqualTo(OrderUtil.SHIPPING_COST_ITEM_NUMBER);
        assertThat(shippingCostLine.getQuantity()).isEqualTo(BigDecimal.valueOf(1));
        assertThat(shippingCostLine.getUnitNetAmount()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostNet());
        assertThat(shippingCostLine.getUnitGrossAmount()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostGross());
        assertThat(shippingCostLine.getLineNetAmount()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostNet());
        assertThat(shippingCostLine.getLineGrossAmount()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostGross());
        assertThat(shippingCostLine.getLineTaxAmount()).isEqualTo(
                salesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostGross()
                        .subtract(salesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostNet()));
        assertThat(shippingCostLine.getTaxRate()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getTotals().getGrandTotalTaxes().get(0).getRate());
    }

    @Test
    void testIsDropshipmentOrderFalse() {

        Order order = Order.builder().orderHeader(OrderHeader.builder().orderFulfillment(K24.getName()).build()).build();
        assertFalse(orderUtil.isDropshipmentOrder(order));
    }

    @Test
    void testIsDropshipmentOrderTrue() {

        Order order = Order.builder().orderHeader(OrderHeader.builder().orderFulfillment(DELTICOM.getName()).build()).build();
        assertTrue(orderUtil.isDropshipmentOrder(order));
    }

    @Test
    void testCreateNewOrderRowToCreateNewOrderRow() {

        final var salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class));
        var lastRowKey = new AtomicInteger(2);

        CoreSalesFinancialDocumentLine orderItem = CoreSalesFinancialDocumentLine.builder()
                .itemNumber(RandomStringUtils.randomAlphabetic(9))
                .quantity(BigDecimal.ONE)
                .unitGrossAmount(BigDecimal.ONE)
                .unitNetAmount(BigDecimal.ONE)
                .lineGrossAmount(BigDecimal.ONE)
                .lineNetAmount(BigDecimal.ONE)
                .taxRate(BigDecimal.TEN)
                .isShippingCost(false)
                .build();

        OrderRows newOrderRow = orderUtil.createNewOrderRow(orderItem, List.of(salesOrder), lastRowKey);

        assertThat(newOrderRow.getSku()).isEqualTo(orderItem.getItemNumber());
        assertThat(newOrderRow.getRowKey()).isEqualTo(3);
        assertThat(newOrderRow.getName()).isEqualTo(orderItem.getItemNumber());
        assertThat(newOrderRow.getQuantity()).isEqualTo(BigDecimal.ONE);
        assertThat(newOrderRow.getTaxRate()).isEqualTo(BigDecimal.TEN);
        assertThat(newOrderRow.getUnitValues().getGoodsValueGross()).isEqualTo(BigDecimal.ONE);
        assertThat(newOrderRow.getUnitValues().getDiscountedGross()).isEqualTo(BigDecimal.ONE);
        assertThat(newOrderRow.getUnitValues().getGoodsValueNet()).isEqualTo(BigDecimal.ONE);
        assertThat(newOrderRow.getUnitValues().getDiscountedNet()).isEqualTo(BigDecimal.ONE);
        assertThat(newOrderRow.getSumValues().getGoodsValueGross()).isEqualTo(BigDecimal.ONE);
        assertThat(newOrderRow.getSumValues().getTotalDiscountedGross()).isEqualTo(BigDecimal.ONE);
        assertThat(newOrderRow.getSumValues().getGoodsValueNet()).isEqualTo(BigDecimal.ONE);
        assertThat(newOrderRow.getSumValues().getTotalDiscountedNet()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void testCreateNewOrderRowToMatchOrderRow() {

        final var salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class));
        salesOrder.setCreatedAt(now());
        final var salesOrder1 = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        salesOrder1.getLatestJson().getOrderRows().get(0).setRowKey(3);
        salesOrder1.setCreatedAt(now().plusMinutes(1));
        salesOrder1.getLatestJson().getOrderRows().get(0).setShippingType("shipping_custom");
        var lastRowKey = new AtomicInteger(3);

        CoreSalesFinancialDocumentLine orderItem = CoreSalesFinancialDocumentLine.builder()
                .itemNumber("1130-0713")
                .description("abc")
                .quantity(BigDecimal.ONE)
                .unitGrossAmount(BigDecimal.ONE)
                .unitNetAmount(BigDecimal.ONE)
                .lineGrossAmount(BigDecimal.ONE)
                .lineNetAmount(BigDecimal.ONE)
                .taxRate(BigDecimal.TEN)
                .isShippingCost(false)
                .build();

        OrderRows matchedOrderRow = orderUtil.createNewOrderRow(orderItem, List.of(salesOrder, salesOrder1), lastRowKey);

        assertThat(matchedOrderRow.getSku()).isEqualTo(orderItem.getItemNumber());
        assertThat(matchedOrderRow.getRowKey()).isEqualTo(3);
        assertThat(matchedOrderRow.getName()).isEqualTo("Sidespejl");
        assertThat(matchedOrderRow.getQuantity()).isEqualTo(BigDecimal.ONE);
        assertThat(matchedOrderRow.getTaxRate()).isEqualTo(BigDecimal.TEN);
        assertThat(matchedOrderRow.getUnitValues().getGoodsValueGross()).isEqualTo(BigDecimal.ONE);
        assertThat(matchedOrderRow.getUnitValues().getDiscountedGross()).isEqualTo(BigDecimal.ONE);
        assertThat(matchedOrderRow.getUnitValues().getGoodsValueNet()).isEqualTo(BigDecimal.ONE);
        assertThat(matchedOrderRow.getUnitValues().getDiscountedNet()).isEqualTo(BigDecimal.ONE);
        assertThat(matchedOrderRow.getSumValues().getGoodsValueGross()).isEqualTo(BigDecimal.ONE);
        assertThat(matchedOrderRow.getSumValues().getTotalDiscountedGross()).isEqualTo(BigDecimal.ONE);
        assertThat(matchedOrderRow.getSumValues().getGoodsValueNet()).isEqualTo(BigDecimal.ONE);
        assertThat(matchedOrderRow.getSumValues().getTotalDiscountedNet()).isEqualTo(BigDecimal.ONE);
        assertThat(matchedOrderRow.getShippingType()).isEqualTo("shipping_custom");
    }

    @Test
    void testGetOrderGroupIdFromOrderNumberWithSeperator() {
        final var orderNumber = "123456789-1";
        var orderGroupId = getOrderGroupIdFromOrderNumber(orderNumber);
        assertThat(orderGroupId).isEqualTo("123456789");
    }

    @Test
    void testGetOrderGroupIdFromOrderNumberWithoutSeperator() {
        final var orderNumber = "123456789";
        var orderGroupId = getOrderGroupIdFromOrderNumber(orderNumber);
        assertThat(orderGroupId).isEqualTo("123456789");
    }

}
