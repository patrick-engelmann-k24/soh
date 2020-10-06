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

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.BPMSalesOrderProcess;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.BPMSalesOrderItemFullfilment;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemMessages;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.glassfish.hk2.api.ValidationInformation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class SalesOrderHubProcessApplicationTest {

    final static String msgSalesOrder = "msg_orderReceivedMarketplace";
    @Autowired
    public ProcessEngine processEngine;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    RepositoryService repositoryService;

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
        final String orderId = getOrderNumber();
        final List<String> orderItems = getOrderItems(orderId, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(_N(Messages.MSG_ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderId)
                        .setVariable(_N(Variables.VAR_ORDER_ID), orderId)
                        .setVariable(_N(Variables.VAR_PAYMENT_TYPE), "creditCard")
                        .setVariable(_N(Variables.VAR_ORDER_VALID), true)
                        .setVariable(_N(Variables.VAR_ORDER_ITEMS), orderItems)
                        .setVariable(_N(Variables.VAR_SHIPMENT_METHOD), "parcel")
                        .correlateWithResult().getProcessInstance();
        assertThat(salesOrderProcessInstance).isWaitingAt(_N(BPMSalesOrderProcess.EVENT_MSG_ORDER_PAYMENT_SECURED));

        sendMessage(_N(BPMSalesOrderProcess.MSG_ORDER_PAYMENT_SECURED), orderId);
        assertThat(salesOrderProcessInstance).isWaitingAt(_N(Activities.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS));

        // send items thru
        sendMessage(_N(ItemMessages.MSG_ITEM_TRANSMITTED), orderId);
        sendMessage(_N(ItemMessages.MSG_PACKING_STARTED), orderId);
        sendMessage(_N(ItemMessages.MSG_TRACKING_ID_RECEIVED), orderId);
        sendMessage(_N(ItemMessages.MSG_ITEM_DELIVERED), orderId);

        BpmnAwareTests.assertThat(salesOrderProcessInstance).hasPassedInOrder(
                _N(BPMSalesOrderProcess.EVENT_START_MSG_ORDER_RECEIVED_FROM_MARKETPLACE),
                _N(BPMSalesOrderProcess.GW_XOR_ORDER_RECEIVED_ECP_OR_MARKETPLACE),
                _N(Messages.EVENT_THROW_MSG_ORDER_CREATED),
                _N(Activities.ACTIVITY_VALIDATE_ORDER),
                _N(BPMSalesOrderProcess.GW_XOR_ORDER_VALID),
                _N(BPMSalesOrderProcess.GW_XOR_ORDER_VALIDATED),
                _N(BPMSalesOrderProcess.EVENT_THROW_MSG_ORDER_VALIDATED),
                _N(BPMSalesOrderProcess.EVENT_MSG_ORDER_PAYMENT_SECURED),
                _N(BPMSalesOrderProcess.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS),
                _N(BPMSalesOrderProcess.EVENT_END_MSG_ORDER_COMPLETED)
        );

        assertThat(salesOrderProcessInstance).isEnded();
    }

    @Test
    public void salesOrderItemDropshipmentCancellationTest() {
        final String orderId = getOrderNumber();
        final String firstItem = orderId + "-item-" + 0;

        final ProcessInstance salesOrderProcessInstance = firstPartOfSalesOrderProcess(orderId);

        List<MessageCorrelationResult> msg_packingStarted = sendMessage(_N(ItemMessages.MSG_PACKING_STARTED), orderId);

        final ProcessInstance firstItemProcessInstance = getFirstOrderItem(firstItem, msg_packingStarted);

        assertThat(firstItemProcessInstance).hasPassedInOrder(_N(BPMSalesOrderItemFullfilment.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS));

        // cancel 1st orderItem
        sendMessage(_N(ItemMessages.MSG_DROPSHIPMENT_CANCELLATION_RECEIVED), orderId, firstItem);
        assertThat(salesOrderProcessInstance).isWaitingAt(_N(Activities.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS));
        // we can't check here for hasPassedInOrder, cause subProcessOrderItemCancellation &
        // ActivityHandleCancellation are not in a defined order
        assertThat(firstItemProcessInstance).hasPassed(
                _N(BPMSalesOrderItemFullfilment.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                _N(BPMSalesOrderItemFullfilment.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                _N(BPMSalesOrderItemFullfilment.GW_XOR_SHIPMENT_METHOD),
                _N(BPMSalesOrderItemFullfilment.EVENT_PACKING_STARTED),
                _N(BPMSalesOrderItemFullfilment.GW_XOR_DROP_SHIPMENT),
                _N(BPMSalesOrderItemFullfilment.EVENT_MSG_DROPSHIPMENT_CANCELLATION_RECEIVED),
                _N(BPMSalesOrderItemFullfilment.GW_XOR_TRACKING_ID_RECEIVED),
                _N(BPMSalesOrderItemFullfilment.EVENT_TRACKING_ID_RECEIVED),
                _N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_CANCELLATION_DROPSHIPMENT),
                _N(BPMSalesOrderItemFullfilment.ACTIVITY_HANDLE_CANCELLATION_DROPSHIPMENT),
                _N(BPMSalesOrderItemFullfilment.EVENT_ORDER_CANCEL),
                _N(BPMSalesOrderItemFullfilment.EVENT_ORDER_ITEM_CANCELLED),
                _N(BPMSalesOrderItemFullfilment.SUB_PROCESS_HANDLE_ORDER_ITEM_CANCELLATION)
        );
        assertThat(firstItemProcessInstance).isEnded();

        // move remaining items
        sendMessage(_N(ItemMessages.MSG_TRACKING_ID_RECEIVED), orderId);
        sendMessage(_N(ItemMessages.MSG_ITEM_DELIVERED), orderId);
        assertThat(salesOrderProcessInstance).isEnded();
    }

    @Test
    public void salesOrderItemShipmentCancellationPossibleTest() {
        final String orderId = getOrderNumber();
        final String firstItem = orderId + "-item-" + 0;

        final ProcessInstance salesOrderProcessInstance = firstPartOfSalesOrderProcess(orderId);

        List<MessageCorrelationResult> msg_packingStarted = sendMessage(_N(ItemMessages.MSG_PACKING_STARTED), orderId);

        final ProcessInstance firstItemProcessInstance = getFirstOrderItem(firstItem, msg_packingStarted);

        assertThat(firstItemProcessInstance).hasPassedInOrder(_N(BPMSalesOrderItemFullfilment.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS));

        // cancel 1st orderItem
        final Map<String, Object> processVariables = Map.of("itemCancellationPossible", true);
        sendMessage(_N(ItemMessages.MSG_ORDER_ITEM_CANCELLATION_RECEIVED), orderId, firstItem, processVariables);

        // main process should stay at the same pos
        assertThat(salesOrderProcessInstance).isWaitingAt(_N(Activities.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS));

        assertThat(firstItemProcessInstance).hasPassed(
                _N(BPMSalesOrderItemFullfilment.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                _N(BPMSalesOrderItemFullfilment.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                _N(BPMSalesOrderItemFullfilment.GW_XOR_SHIPMENT_METHOD),
                _N(BPMSalesOrderItemFullfilment.EVENT_PACKING_STARTED),
                _N(BPMSalesOrderItemFullfilment.GW_XOR_DROP_SHIPMENT),
                _N(BPMSalesOrderItemFullfilment.EVENT_MSG_SHIPMENT_CANCELLATION_RECEIVED),
                _N(BPMSalesOrderItemFullfilment.ACTIVITY_CHECK_CANCELLATION_POSSIBLE),
                _N(BPMSalesOrderItemFullfilment.GW_XOR_CANCELLATION_POSSIBLE),
                _N(BPMSalesOrderItemFullfilment.ACTIVITY_HANDLE_CANCELLATION_SHIPMENT),
                _N(BPMSalesOrderItemFullfilment.EVENT_ORDER_ITEM_SHIPMENT_CANCELLED)
        );
        assertThat(firstItemProcessInstance).isEnded();

        // move remaining items
        sendMessage(_N(ItemMessages.MSG_TRACKING_ID_RECEIVED), orderId);
        sendMessage(_N(ItemMessages.MSG_ITEM_DELIVERED), orderId);
        assertThat(salesOrderProcessInstance).isEnded();
    }

    protected ProcessInstance getFirstOrderItem(final String firstItem, final List<MessageCorrelationResult> correlationResultList) {
        String firstItemProcessInstanceId = correlationResultList.get(0).getExecution().getProcessInstanceId();
        final ProcessInstance firstItemProcessInstance = getProcessInstanceQuery()
                .processInstanceId(firstItemProcessInstanceId)
                .singleResult();

        assertThat(firstItemProcessInstance).isActive();
        assertThat(firstItemProcessInstance)
                .hasVariables(
                        _N(Variables.VAR_ORDER_ID),
                        _N(Variables.VAR_SHIPMENT_METHOD),
                        _N(Variables.VAR_ITEM_ID)
                );

        return firstItemProcessInstance;
    }

    protected ProcessInstance firstPartOfSalesOrderProcess(final String orderId) {
        final List<String> orderItems = getOrderItems(orderId, 5);
        final ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(_N(Messages.MSG_ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderId)
                        .setVariable(_N(Variables.VAR_ORDER_ID), orderId)
                        .setVariable(_N(Variables.VAR_PAYMENT_TYPE), "creditCard")
                        .setVariable(_N(Variables.VAR_ORDER_VALID), true)
                        .setVariable(_N(Variables.VAR_ORDER_ITEMS), orderItems)
                        .setVariable(_N(Variables.VAR_SHIPMENT_METHOD), "parcel")
                        .correlateWithResult().getProcessInstance();
        assertThat(salesOrderProcessInstance).isActive();
        assertThat(salesOrderProcessInstance).isWaitingAt(_N(BPMSalesOrderProcess.EVENT_MSG_ORDER_PAYMENT_SECURED));
        assertThat(salesOrderProcessInstance).hasPassed(_N(Activities.ACTIVITY_VALIDATE_ORDER));

        runtimeService.createMessageCorrelation(_N(BPMSalesOrderProcess.MSG_ORDER_PAYMENT_SECURED))
                .processInstanceBusinessKey(orderId)
                .setVariable(_N(Variables.VAR_PAYMENT_STATUS), "captured")
                .correlateWithResult().getProcessInstance();

        assertThat(salesOrderProcessInstance).hasPassed(
                _N(Messages.EVENT_THROW_MSG_ORDER_CREATED),
                _N(BPMSalesOrderProcess.EVENT_THROW_MSG_ORDER_VALIDATED)
        );

        assertThat(salesOrderProcessInstance).isWaitingAt(_N(Activities.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS));

        // start the subprocess
        // move all items to packing started
        sendMessage(_N(ItemMessages.MSG_ITEM_TRANSMITTED), orderId);
        return salesOrderProcessInstance;
    }

    final List<String> getOrderItems(final String orderId, final int number) {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            result.add(orderId + "-item-" + i);
        }
        return result;
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

    final MessageCorrelationResult sendMessage(final String message, final String orderId, final String orderItem) {
        return sendMessage(message, orderId, orderItem, Collections.emptyMap());
    }

    final MessageCorrelationResult sendMessage(final String message, final String orderId, final String orderItem,
                                               final Map<String, Object> processVariables) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message)
                .processInstanceVariableEquals("orderId", orderId)
                .processInstanceVariableEquals("orderItemId", orderItem);
        if (!processVariables.isEmpty())
            builder.setVariables(processVariables);

        return builder
                .correlateWithResult();
    }

    final List<MessageCorrelationResult> sendMessage(String message, String orderId) {
        return runtimeService.createMessageCorrelation(message)
                .processInstanceVariableEquals("orderId", orderId)
                .correlateAllWithResult();
    }

    final List<MessageCorrelationResult> sendMessage(String message) {
        return runtimeService.createMessageCorrelation(message)
                .correlateAllWithResult();
    }

    final ProcessInstanceQuery getProcessInstanceQuery() {
        return runtimeService.createProcessInstanceQuery();
    }

    final String _N(BpmItem item) {
        return item.getName();
    }

}
