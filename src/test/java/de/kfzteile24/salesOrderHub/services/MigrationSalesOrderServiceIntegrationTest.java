package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.helper.ObjectUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class MigrationSalesOrderServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    @Autowired
    private TimedPollingService timerService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private BpmUtil bpmUtil;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TimedPollingService timedPollingService;
    @Autowired
    private ObjectUtil objectUtil;
    @Autowired
    private SalesOrderService salesOrderService;
    @Autowired
    private SnsPublishService snsPublishService;
    @Autowired
    private SalesOrderMapper salesOrderMapper;
    @Autowired
    private MessageErrorHandler messageErrorHandler;
    @Autowired
    private MigrationSalesOrderService migrationSalesOrderService;
    @Autowired
    private TimedPollingService pollingService;

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

        String orderRawMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(orderRawMessage);
        Order originalOrder = getOrder(orderRawMessage);

        migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(orderRawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertFalse(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(7),
                () -> camundaHelper.checkIfActiveProcessExists(order.getOrderHeader().getOrderNumber())));

        SalesOrder updated = salesOrderService.getOrderByOrderNumber(order.getOrderHeader().getOrderNumber()).orElse(null);
        assertNotNull(updated);
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());
        assertEquals(order, updated.getLatestJson());
        assertEquals(originalOrder, updated.getOriginalOrder());

        wireMockServer.stop();
    }
    @SneakyThrows
    @Test
    void testHandleMigrationCoreSalesOrderCreatedDuplicateOrder() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        Order originalOrder = getOrder(orderRawMessage);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber()))
        );

        migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(orderRawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timedPollingService.pollWithDefaultTiming(
                () -> camundaHelper.checkIfActiveProcessExists(order.getOrderHeader().getOrderNumber())));

        SalesOrder updated = salesOrderService.getOrderByOrderNumber(order.getOrderHeader().getOrderNumber()).orElse(null);
        assertNotNull(updated);
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());
        assertEquals(order, updated.getLatestJson());
        assertEquals(originalOrder, updated.getOriginalOrder());
    }

    @AfterEach
    public void cleanup() {
        pollingService.retry(() -> salesOrderRepository.deleteAll());
    }
}