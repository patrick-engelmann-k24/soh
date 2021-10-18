/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.kfzteile24.salesOrderHub;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowActivities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowGateways;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.NONE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class SalesOrderHubProcessApplicationIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private CamundaHelper camundaHelper;

    @Autowired
    private TimedPollingService pollingService;

    private SalesOrder testOrder;

    @BeforeEach
    public void setup() {
        init(processEngine);
        testOrder = salesOrderUtil.createNewSalesOrder();
    }

    @Test
    public void startUpTest() {
        // context init test
        // test if the processEngine is configured correct and we can use it here.
        assertEquals("default", processEngine.getName());
        assertNotNull(runtimeService);
    }

    /**
     * Happy case test. Follow the process as intended
     */
    @Test
    public void salesOrderItemPassThruTest() {
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_MARKETPLACE.getName())
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(Variables.ORDER_NUMBER.getName(), orderNumber)
                        .setVariable(Variables.PAYMENT_TYPE.getName(), CREDIT_CARD.getName())
                        .setVariable(Variables.ORDER_VALID.getName(), true)
                        .setVariable(Variables.ORDER_ROWS.getName(), orderItems)
                        .setVariable(Variables.SHIPMENT_METHOD.getName(), REGULAR.getName())
                        .correlateWithResult().getProcessInstance();

        assertTrue(util.isProcessWaitingAtExpectedToken(salesOrderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName(), orderNumber);
        assertThat(salesOrderProcessInstance).isWaitingAt(Activities.ORDER_ROW_FULFILLMENT_PROCESS.getName());

        // send items thru
        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS.getName(), orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED.getName(), orderNumber);
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED.getName(), orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED.getName(), orderNumber);

        assertThat(salesOrderProcessInstance).isEnded().hasPassed(Events.END_MSG_ORDER_COMPLETED.getName());
    }

    @Test
    public void salesOrderItemShipmentCancellationPossibleTest() {
        final var salesOrder = salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final String orderNumber = salesOrder.getOrderNumber();
        final String skuToCancel = salesOrder.getLatestJson().getOrderRows().get(0).getSku();

        final ProcessInstance salesOrderProcessInstance = firstPartOfSalesOrderProcess(salesOrder.getLatestJson());

        List<MessageCorrelationResult> msg_packingStarted = util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);

        final ProcessInstance firstItemProcessInstance = getFirstOrderItem(msg_packingStarted);

        assertThat(firstItemProcessInstance).hasPassedInOrder(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName());

        // cancel 1st orderItem
        final Map<String, Object> processVariables = Map.of(RowVariables.ROW_CANCELLATION_POSSIBLE.getName(), true);
        util.sendMessage(RowMessages.ORDER_ROW_CANCELLATION_RECEIVED, orderNumber, skuToCancel, processVariables);

        // main process should stay at the same pos
        assertTrue(util.isProcessWaitingAtExpectedToken(salesOrderProcessInstance,
                Activities.ORDER_ROW_FULFILLMENT_PROCESS.getName()));

        assertThat(firstItemProcessInstance).hasPassed(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                RowGateways.XOR_SHIPMENT_METHOD.getName(),
                RowEvents.PACKING_STARTED.getName(),
                RowEvents.MSG_ROW_CANCELLATION_RECEIVED.getName(),
                RowActivities.CHECK_CANCELLATION_POSSIBLE.getName(),
                RowGateways.XOR_CANCELLATION_POSSIBLE.getName(),
                RowActivities.HANDLE_CANCELLATION_SHIPMENT.getName()
        );
        assertThat(firstItemProcessInstance).isEnded();

        // move remaining items
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);
        assertThat(salesOrderProcessInstance).isEnded();
    }

    @Test
    public void salesOrderCancellationTest() {
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_MARKETPLACE.getName())
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(Variables.ORDER_NUMBER.getName(), orderNumber)
                        .setVariable(Variables.PAYMENT_TYPE.getName(), CREDIT_CARD.getName())
                        .setVariable(Variables.ORDER_VALID.getName(), true)
                        .setVariable(Variables.ORDER_ROWS.getName(), orderItems)
                        .setVariable(Variables.SHIPMENT_METHOD.getName(), REGULAR.getName())
                        .correlateWithResult().getProcessInstance();

        assertTrue(util.isProcessWaitingAtExpectedToken(salesOrderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));

        runtimeService.createMessageCorrelation(Messages.ORDER_CANCELLATION_RECEIVED.getName())
                .processInstanceBusinessKey(orderNumber)
                .correlateWithResult();

        pollingService.pollWithDefaultTiming(() -> {
            assertThat(salesOrderProcessInstance)
                    .isEnded()
                    .hasPassed(Events.ORDER_CANCELLATION_RECEIVED.getName());
            return true;
        });
    }

    @Test
    public void noSubprocessesAreCreatedForOrderRowsWithShippingTypeNone() {
        final var salesOrder = salesOrderUtil.createPersistedSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        final var processInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        final var virtualOrderRowSkus = (List<String>) runtimeService.getVariable(processInstance.getProcessInstanceId(),
                Variables.VIRTUAL_ORDER_ROWS.getName());
        assertNotNull(virtualOrderRowSkus);
        assertEquals(1, virtualOrderRowSkus.size());

        final var expectedVirtualSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(orderRow -> orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());
        Assertions.assertThat(virtualOrderRowSkus).isEqualTo(expectedVirtualSkus);

        final var orderRowSkus = runtimeService.getVariable(processInstance.getProcessInstanceId(),
                Variables.ORDER_ROWS.getName());

        final var expectedOrderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(orderRow -> !orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());
        Assertions.assertThat(orderRowSkus).isEqualTo(expectedOrderRowSkus);

        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));

        util.sendMessage(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName(), salesOrder.getOrderNumber());

        salesOrder.getLatestJson().getOrderRows()
                .forEach(orderRow -> {
                    Assertions.assertThat(
                            camundaHelper.checkIfItemProcessExists(salesOrder.getOrderNumber(), orderRow.getSku()))
                            .isEqualTo(!NONE.getName().equals(orderRow.getShippingType()));

                });
        assertFalse(camundaHelper.checkIfItemProcessExists(salesOrder.getOrderNumber(), virtualOrderRowSkus.get(0)));
    }

    protected ProcessInstance getFirstOrderItem(final List<MessageCorrelationResult> correlationResultList) {
        String firstItemProcessInstanceId = correlationResultList.get(0).getExecution().getProcessInstanceId();
        final ProcessInstance firstItemProcessInstance = getProcessInstanceQuery()
                .processInstanceId(firstItemProcessInstanceId)
                .singleResult();

        assertThat(firstItemProcessInstance).isActive();
        assertThat(firstItemProcessInstance)
                .hasVariables(
                        Variables.ORDER_NUMBER.getName(),
                        Variables.SHIPMENT_METHOD.getName(),
                        RowVariables.ORDER_ROW_ID.getName()
                );

        return firstItemProcessInstance;
    }

    protected ProcessInstance firstPartOfSalesOrderProcess(final Order order) {
        final var orderNumber = order.getOrderHeader().getOrderNumber();
        final var skus = order.getOrderRows().stream().map(OrderRows::getSku).collect(Collectors.toList());
        final ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_MARKETPLACE.getName())
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(Variables.ORDER_NUMBER.getName(), orderNumber)
                        .setVariable(Variables.PAYMENT_TYPE.getName(), CREDIT_CARD.getName())
                        .setVariable(Variables.ORDER_VALID.getName(), true)
                        .setVariable(Variables.ORDER_ROWS.getName(), skus)
                        .setVariable(Variables.SHIPMENT_METHOD.getName(), REGULAR.getName())
                        .correlateWithResult().getProcessInstance();
        assertThat(salesOrderProcessInstance).isActive();


        pollingService.pollWithDefaultTiming(() -> {
            assertThat(salesOrderProcessInstance)
                    .hasPassed(Events.THROW_MSG_ORDER_CREATED.getName())
                    .isWaitingAt(Events.MSG_ORDER_PAYMENT_SECURED.getName());
            return true;
        });

        runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName())
                .processInstanceBusinessKey(orderNumber)
                .setVariable(Variables.PAYMENT_STATUS.getName(), "captured")
                .correlateWithResult().getProcessInstance();

        assertThat(salesOrderProcessInstance).isWaitingAt(Activities.ORDER_ROW_FULFILLMENT_PROCESS.getName());

        // start the subprocess
        // move all items to packing started
        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS.getName(), orderNumber);
        return salesOrderProcessInstance;
    }

    private ProcessInstanceQuery getProcessInstanceQuery() {
        return runtimeService.createProcessInstanceQuery();
    }

}
