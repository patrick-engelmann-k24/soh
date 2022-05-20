package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class DropshipmentOrderServiceIntegrationTest {

    @Autowired
    private DropshipmentOrderService dropshipmentOrderService;
    @SpyBean
    private CamundaHelper camundaHelper;
    @SpyBean
    private SalesOrderService salesOrderService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private SalesOrderInvoiceRepository salesOrderInvoiceRepository;

    @Test
    void testHandleDropShipmentOrderConfirmed() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));

        doNothing().when(camundaHelper).correlateMessage(any(), any(), any());

        var message = DropshipmentPurchaseOrderBookedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .externalOrderNumber("13.2")
                .build();
        dropshipmentOrderService.handleDropShipmentOrderConfirmed(message);

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals("13.2", updatedOrder.getLatestJson().getOrderHeader().getOrderNumberExternal());
    }

    @Test
    void testHandleDropShipmentOrderTrackingInformationReceived() {

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        salesOrderService.save(salesOrder, Action.ORDER_CREATED);

        doNothing().when(camundaHelper).correlateMessage(any(), any(), any());

        var message = DropshipmentShipmentConfirmedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .items(Set.of(ShipmentItem.builder()
                        .productNumber("sku-1")
                        .parcelNumber("00F8F0LT")
                        .trackingLink("http://abc1")
                        .serviceProviderName("abc1")
                        .build(), ShipmentItem.builder()
                        .productNumber("sku-3")
                        .parcelNumber("00F8F0LT2")
                        .trackingLink("http://abc2")
                        .serviceProviderName("abc2")
                        .build()))
                .build();

        dropshipmentOrderService.handleDropShipmentOrderTrackingInformationReceived(message);

        var optUpdatedSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber());
        assertThat(optUpdatedSalesOrder).isNotEmpty();
        var updatedSalesOrder = optUpdatedSalesOrder.get();
        var updatedOrderRows = updatedSalesOrder.getLatestJson().getOrderRows();
        assertThat(updatedOrderRows).hasSize(3);

        var sku1Row = updatedOrderRows.get(0);
        var sku2Row = updatedOrderRows.get(1);
        var sku3Row = updatedOrderRows.get(2);

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(sku1Row.getSku()).as("sku-1").isEqualTo("sku-1");
            softly.assertThat(sku1Row.getShippingProvider()).as("Service provider name").isEqualTo("abc1");
            softly.assertThat(sku1Row.getTrackingNumbers()).as("Size of tracking numbers sku-1").hasSize(1);
            softly.assertThat(sku1Row.getTrackingNumbers().get(0)).as("sku-1 tracking number").isEqualTo("00F8F0LT");

            softly.assertThat(sku2Row.getSku()).as("sku-2").isEqualTo("sku-2");
            softly.assertThat(sku2Row.getTrackingNumbers()).as("Size of tracking numbers sku-2").isNull();

            softly.assertThat(sku3Row.getSku()).as("sku-3").isEqualTo("sku-3");
            softly.assertThat(sku3Row.getShippingProvider()).as("Service provider name").isEqualTo("abc2");
            softly.assertThat(sku3Row.getTrackingNumbers()).as("Size of tracking numbers sku-3").hasSize(1);
            softly.assertThat(sku3Row.getTrackingNumbers().get(0)).as("sku-3 tracking number").isEqualTo("00F8F0LT2");
        }
        assertThat(updatedSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()).hasSize(18);
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return java.nio.file.Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                        .toURI()));
    }

    @AfterEach
    public void cleanup() {
        salesOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
        salesOrderInvoiceRepository.deleteAll();
    }
}