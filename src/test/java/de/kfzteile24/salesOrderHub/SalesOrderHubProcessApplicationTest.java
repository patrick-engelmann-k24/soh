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
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.*;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class SalesOrderHubProcessApplicationTest {

    @Autowired
    public ProcessEngine processEngine;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    BpmUtil util;

    @Autowired
    SalesOrderUtil salesOrderUtil;

    private SalesOrder testOrder;

    @BeforeEach
    public void setup() {
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
        final List<String> orderItems = util.getOrderItems(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(Messages.ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(Variables.ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(Variables.PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(Variables.ORDER_VALID), true)
                        .setVariable(util._N(Variables.ORDER_ITEMS), orderItems)
                        .setVariable(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Events.MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(util._N(Messages.ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);
        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Activities.ORDER_ITEM_FULFILLMENT_PROCESS));

        // send items thru
        util.sendMessage(util._N(ItemMessages.ITEM_TRANSMITTED_TO_LOGISTICS), orderNumber);
        util.sendMessage(util._N(ItemMessages.PACKING_STARTED), orderNumber);
        util.sendMessage(util._N(ItemMessages.TRACKING_ID_RECEIVED), orderNumber);
        util.sendMessage(util._N(ItemMessages.ITEM_SHIPPED), orderNumber);

        assertThat(salesOrderProcessInstance).isEnded().hasPassed(util._N(Events.END_MSG_ORDER_COMPLETED));
    }

    @Test
    public void salesOrderItemShipmentCancellationPossibleTest() {
        final String orderNumber = testOrder.getOrderNumber();
        final String firstItem = orderNumber + "-item-" + 0;

        final ProcessInstance salesOrderProcessInstance = firstPartOfSalesOrderProcess(orderNumber);

        List<MessageCorrelationResult> msg_packingStarted = util.sendMessage(util._N(ItemMessages.PACKING_STARTED), orderNumber);

        final ProcessInstance firstItemProcessInstance = getFirstOrderItem(firstItem, msg_packingStarted);

        assertThat(firstItemProcessInstance).hasPassedInOrder(util._N(ItemEvents.ITEM_TRANSMITTED_TO_LOGISTICS));

        // cancel 1st orderItem
        final Map<String, Object> processVariables = Map.of("itemCancellationPossible", true);
        util.sendMessage(util._N(ItemMessages.ORDER_ITEM_CANCELLATION_RECEIVED), orderNumber, firstItem, processVariables);

        // main process should stay at the same pos
        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Activities.ORDER_ITEM_FULFILLMENT_PROCESS));

        assertThat(firstItemProcessInstance).hasPassed(
                util._N(ItemEvents.START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.PACKING_STARTED),
                util._N(ItemEvents.MSG_SHIPMENT_CANCELLATION_RECEIVED),
                util._N(ItemActivities.CHECK_CANCELLATION_POSSIBLE),
                util._N(ItemGateways.XOR_CANCELLATION_POSSIBLE),
                util._N(ItemActivities.HANDLE_CANCELLATION_SHIPMENT)
        );
        assertThat(firstItemProcessInstance).isEnded();

        // move remaining items
        util.sendMessage(util._N(ItemMessages.TRACKING_ID_RECEIVED), orderNumber);
        util.sendMessage(util._N(ItemMessages.ITEM_SHIPPED), orderNumber);
        assertThat(salesOrderProcessInstance).isEnded();
    }

    @Test
    public void salesOrderCancellationTest() {
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderItems(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(Messages.ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(Variables.ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(Variables.PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(Variables.ORDER_VALID), true)
                        .setVariable(util._N(Variables.ORDER_ITEMS), orderItems)
                        .setVariable(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Events.MSG_ORDER_PAYMENT_SECURED));

        runtimeService.createMessageCorrelation(util._N(Messages.ORDER_CANCELLATION_RECEIVED))
                .processInstanceBusinessKey(orderNumber)
                .correlateWithResult();

        assertThat(salesOrderProcessInstance)
                .isEnded()
                .hasPassed(util._N(Events.ORDER_CANCELLATION_RECEIVED));

    }

    protected ProcessInstance getFirstOrderItem(final String firstItem, final List<MessageCorrelationResult> correlationResultList) {
        String firstItemProcessInstanceId = correlationResultList.get(0).getExecution().getProcessInstanceId();
        final ProcessInstance firstItemProcessInstance = getProcessInstanceQuery()
                .processInstanceId(firstItemProcessInstanceId)
                .singleResult();

        assertThat(firstItemProcessInstance).isActive();
        assertThat(firstItemProcessInstance)
                .hasVariables(
                        util._N(Variables.ORDER_NUMBER),
                        util._N(Variables.SHIPMENT_METHOD),
                        util._N(ItemVariables.ORDER_ITEM_ID)
                );

        return firstItemProcessInstance;
    }

    protected ProcessInstance firstPartOfSalesOrderProcess(final String orderNumber) {
        final List<String> orderItems = util.getOrderItems(orderNumber, 5);
        final ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(Messages.ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(Variables.ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(Variables.PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(Variables.ORDER_VALID), true)
                        .setVariable(util._N(Variables.ORDER_ITEMS), orderItems)
                        .setVariable(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();
        assertThat(salesOrderProcessInstance).isActive();

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(salesOrderProcessInstance)
                .hasPassed(util._N(Events.THROW_MSG_ORDER_CREATED))
                .isWaitingAt(util._N(Events.MSG_ORDER_PAYMENT_SECURED));

        runtimeService.createMessageCorrelation(util._N(Messages.ORDER_RECEIVED_PAYMENT_SECURED))
                .processInstanceBusinessKey(orderNumber)
                .setVariable(util._N(Variables.PAYMENT_STATUS), "captured")
                .correlateWithResult().getProcessInstance();

        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Activities.ORDER_ITEM_FULFILLMENT_PROCESS));

        // start the subprocess
        // move all items to packing started
        util.sendMessage(util._N(ItemMessages.ITEM_TRANSMITTED_TO_LOGISTICS), orderNumber);
        return salesOrderProcessInstance;
    }

    final String getOrderNumber() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 8;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }

    final ProcessInstanceQuery getProcessInstanceQuery() {
        return runtimeService.createProcessInstanceQuery();
    }

}
