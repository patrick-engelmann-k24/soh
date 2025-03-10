package de.kfzteile24.salesOrderHub.services;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.copyOrderJson;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class MigrationSalesOrderServiceIntegrationTest extends AbstractIntegrationTest {


    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private TimedPollingService timedPollingService;
    @Autowired
    private SalesOrderService salesOrderService;
    @Autowired
    private MigrationSalesOrderService migrationSalesOrderService;
    @Autowired
    private TimedPollingService pollingService;
    @Autowired
    private SalesOrderProcessService salesOrderProcessService;

    @Test
    void testHandleMigrationCoreSalesOrderCreated() throws URISyntaxException, IOException {

        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("examples/product/DZN.json")).toURI();
        byte[] bytes = Files.readAllBytes(Paths.get(uri));
        WireMockServer wireMockServer = new WireMockServer(18080);
        wireMockServer.start();
        wireMockServer.stubFor(WireMock.post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse().withBody("{ \"access_token\": \"fake_access_token\" }")));
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/json/v15/?sku=1130-0713"))
                .willReturn(aResponse().withBody(bytes)));

        var message = getObjectByResource("ecpOrderMessage.json", Order.class);
        Order originalOrder = copyOrderJson(message);
        var messageWrapper = MessageWrapper.builder().build();

        migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(message, messageWrapper);

        assertFalse(timedPollingService.pollWithDefaultTiming(
                () -> camundaHelper.checkIfActiveProcessExists(SALES_ORDER_PROCESS, message.getOrderHeader().getOrderNumber())));

        SalesOrder updated = salesOrderService.getOrderByOrderNumber(message.getOrderHeader().getOrderNumber()).orElse(null);
        assertNotNull(updated);
        message.getOrderHeader().setOrderGroupId(message.getOrderHeader().getOrderNumber());
        assertEquals(message, updated.getLatestJson());
        assertEquals(originalOrder, updated.getOriginalOrder());

        wireMockServer.stop();
    }

    @SneakyThrows
    @Test
    void testHandleMigrationCoreSalesOrderCreatedDuplicateOrder() {

        var message = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        var messageWrapper = MessageWrapper.builder().build();
        Order originalOrder = copyOrderJson(message);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(message));
        salesOrderProcessService.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(SALES_ORDER_PROCESS, salesOrder.getOrderNumber()))
        );

        migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(message, messageWrapper);

        assertTrue(timedPollingService.pollWithDefaultTiming(
                () -> camundaHelper.checkIfActiveProcessExists(SALES_ORDER_PROCESS, message.getOrderHeader().getOrderNumber())));

        SalesOrder updated = salesOrderService.getOrderByOrderNumber(message.getOrderHeader().getOrderNumber()).orElse(null);
        assertNotNull(updated);
        message.getOrderHeader().setOrderGroupId(message.getOrderHeader().getOrderNumber());
        assertEquals(message, updated.getLatestJson());
        assertEquals(originalOrder, updated.getOriginalOrder());
    }

    @BeforeEach
    public void prepare() {
        pollingService.retry(() -> salesOrderRepository.deleteAllInBatch());
    }

    @AfterEach
    public void cleanup() {
        pollingService.retry(() -> salesOrderRepository.deleteAllInBatch());
    }
}