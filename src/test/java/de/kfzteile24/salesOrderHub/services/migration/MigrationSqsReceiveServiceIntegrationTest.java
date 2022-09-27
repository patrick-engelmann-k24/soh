package de.kfzteile24.salesOrderHub.services.migration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.ObjectUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceNumberCounterService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author stefand
 */

@Slf4j
class MigrationSqsReceiveServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    @Autowired
    private MigrationSqsReceiveService migrationSqsReceiveService;
    @Autowired
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;
    @Autowired
    private TimedPollingService timedPollingService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private InvoiceNumberCounterRepository invoiceNumberCounterRepository;
    @Autowired
    private InvoiceNumberCounterService invoiceNumberCounterService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private BpmUtil bpmUtil;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private ObjectUtil objectUtil;

    @BeforeEach
    public void setup() {
        super.setUp();
        bpmUtil.cleanUp();
        salesOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
        invoiceNumberCounterRepository.deleteAll();
        invoiceNumberCounterService.init();
    }

    @Test
    void testQueueListenerMigrationCoreSalesOrderCreated() throws URISyntaxException, IOException {

        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("examples/product/DZN.json")).toURI();
        byte[] bytes = Files.readAllBytes(Paths.get(uri));
        WireMockServer wireMockServer = new WireMockServer(18080);
        wireMockServer.start();
        wireMockServer.stubFor(WireMock.post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse().withBody("{ \"access_token\": \"fake_access_token\" }")));
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/json/v15/?sku=1130-0713"))
                .willReturn(aResponse().withBody(bytes)));

        String orderRawMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(orderRawMessage);
        Order originalOrder = getOrder(orderRawMessage);

        migrationSqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(orderRawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertFalse(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(7),
                () -> camundaHelper.checkIfActiveProcessExists(order.getOrderHeader().getOrderNumber())));

        SalesOrder updated = salesOrderService.getOrderByOrderNumber(order.getOrderHeader().getOrderNumber()).orElse(null);
        assertNotNull(updated);
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());
        assertEquals(order, updated.getLatestJson());
        assertEquals(originalOrder, updated.getOriginalOrder());

        wireMockServer.stop();
    }

    @Test
    void testQueueListenerMigrationCoreSalesOrderCreatedDuplicateOrder() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        Order originalOrder = getOrder(orderRawMessage);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber()))
        );

        migrationSqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(orderRawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timedPollingService.pollWithDefaultTiming(
                () -> camundaHelper.checkIfActiveProcessExists(order.getOrderHeader().getOrderNumber())));

        SalesOrder updated = salesOrderService.getOrderByOrderNumber(order.getOrderHeader().getOrderNumber()).orElse(null);
        assertNotNull(updated);
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());
        assertEquals(order, updated.getLatestJson());
        assertEquals(originalOrder, updated.getOriginalOrder());
    }

    @Test
    void testQueueListenerMigrationCoreSalesInvoiceCreated() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrder();

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";
        String migrationInvoiceMsg = readResource("examples/coreSalesInvoiceCreatedMultipleItems.json");

        //Replace order number with randomly created order number as expected
        migrationInvoiceMsg = migrationInvoiceMsg.replace("524001248", originalOrderNumber);

        migrationSqsReceiveService.queueListenerMigrationCoreSalesInvoiceCreated(migrationInvoiceMsg, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku1)));
        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku2)));
        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku3)));
    }

    @Test
    void testQueueListenerMigrationCoreSalesInvoiceCreatedDuplicateSubsequentOrder() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrder();

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";
        String rowSku = "2010-10183";
        String invoiceMsg = readResource("examples/coreSalesInvoiceCreatedOneItem.json");

        //Replace order number with randomly created order number as expected
        invoiceMsg = invoiceMsg.replace("524001248", originalOrderNumber);

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(invoiceMsg, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));

        migrationSqsReceiveService.queueListenerMigrationCoreSalesInvoiceCreated(invoiceMsg, "Migration Delivery", 1);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));

        verify(snsPublishService).publishMigrationOrderRowCancelled(eq(originalOrderNumber), eq(rowSku));
        verify(snsPublishService).publishMigrationOrderCreated(eq(newOrderNumberCreatedInSoh));
    }

    @Test
    @DisplayName("IT migration core sales credit note created event handling")
    void testQueueListenerMigrationCoreSalesCreditNoteCreated(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = salesOrderUtil.createSalesOrderForMigrationInvoiceTest();
        var orderNumber = salesOrder.getOrderNumber();
        var creditNumber = "876130";

        var coreReturnDeliveryNotePrinted = readResource("examples/coreSalesCreditNoteCreated.json");
        migrationSqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishReturnOrderCreatedEvent(argThat(
                salesOrderReturn -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo(createOrderNumberInSOH(orderNumber, creditNumber));
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo(orderNumber);
                    return true;
                }
        ));
    }

    @Test
    @DisplayName("IT migration core sales credit note created event handling if related return order already exists")
    void testQueueListenerMigrationCoreSalesCreditNoteCreatedDuplicateReturnOrder(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = salesOrderUtil.createSalesOrderForMigrationInvoiceTest();
        var orderNumber = salesOrder.getOrderNumber();
        var creditNumber = "876130";

        var coreReturnDeliveryNotePrinted = readResource("examples/coreSalesCreditNoteCreated.json");
        financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        migrationSqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(argThat(
                (SalesOrderReturn salesOrderReturn) -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo(createOrderNumberInSOH(orderNumber, creditNumber));
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo(orderNumber);
                    return true;
                }
        ));
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                        .toURI()));
    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        timedPollingService.retry(() -> salesOrderRepository.deleteAll());
        timedPollingService.retry(() -> auditLogRepository.deleteAll());
        timedPollingService.retry(() -> bpmUtil.cleanUp());
        timedPollingService.retry(() -> invoiceNumberCounterRepository.deleteAll());
        invoiceNumberCounterService.init();
    }
}
