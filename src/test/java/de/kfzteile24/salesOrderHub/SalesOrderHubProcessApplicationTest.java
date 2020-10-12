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

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.*;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.BPMSalesOrderItemFullfilment;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables.VAR_ITEM_CANCELLATION_POSSIBLE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.PaymentType.PAYMENT_CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ShipmentMethod.PARCEL;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
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
        final String orderId = util.getRandomOrderNumber();
        final List<String> orderItems = getOrderItems(orderId, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(Messages.MSG_ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderId)
                        .setVariable(util._N(Variables.VAR_ORDER_ID), orderId)
                        .setVariable(util._N(Variables.VAR_PAYMENT_TYPE), PAYMENT_CREDIT_CARD.getName())
                        .setVariable(util._N(Variables.VAR_ORDER_VALID), true)
                        .setVariable(util._N(Variables.VAR_ORDER_ITEMS), orderItems)
                        .setVariable(util._N(Variables.VAR_SHIPMENT_METHOD), PARCEL.getName())
                        .correlateWithResult().getProcessInstance();
        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Activities.EVENT_MSG_ORDER_PAYMENT_SECURED));

        util.sendMessage(Messages.MSG_ORDER_RECEIVED_PAYMENT_SECURED, orderId);
        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Activities.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS));

        // send items thru
        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED, orderId);
        util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderId);
        util.sendMessage(ItemMessages.MSG_TRACKING_ID_RECEIVED, orderId);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderId);

        BpmnAwareTests.assertThat(salesOrderProcessInstance).hasPassedInOrder(
                util._N(Activities.EVENT_START_MSG_ORDER_RECEIVED_FROM_MARKETPLACE),
                util._N(Gateways.GW_XOR_ORDER_RECEIVED_ECP_OR_MARKETPLACE),
                util._N(Activities.EVENT_CATCH_MSG_ORDER_CREATED),
                util._N(Activities.ACTIVITY_VALIDATE_ORDER),
                util._N(Gateways.GW_XOR_ORDER_VALID),
                util._N(Gateways.GW_XOR_ORDER_VALIDATED),
                util._N(Activities.EVENT_THROW_MSG_ORDER_VALIDATED),
                util._N(Activities.EVENT_MSG_ORDER_PAYMENT_SECURED),
                util._N(Activities.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(Activities.EVENT_END_MSG_ORDER_COMPLETED)
        );

        assertThat(salesOrderProcessInstance).isEnded();
    }

    @Test
    public void salesOrderItemDropshipmentCancellationTest() {
        final String orderId = util.getRandomOrderNumber();
        final String firstItem = orderId + "-item-" + 0;

        final ProcessInstance salesOrderProcessInstance = firstPartOfSalesOrderProcess(orderId);

        List<MessageCorrelationResult> msg_packingStarted = util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderId);

        final ProcessInstance firstItemProcessInstance = getFirstOrderItem(firstItem, msg_packingStarted);

        assertThat(firstItemProcessInstance).hasPassedInOrder(util._N(BPMSalesOrderItemFullfilment.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS));

        // cancel 1st orderItem
        util.sendMessage(ItemMessages.MSG_DROPSHIPMENT_CANCELLATION_RECEIVED, orderId, firstItem);
        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Activities.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS));
        // we can't check here for hasPassedInOrder, cause subProcessOrderItemCancellation &
        // ActivityHandleCancellation are not in a defined order
        assertThat(firstItemProcessInstance).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(BPMSalesOrderItemFullfilment.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(BPMSalesOrderItemFullfilment.GW_XOR_SHIPMENT_METHOD),
                util._N(BPMSalesOrderItemFullfilment.EVENT_PACKING_STARTED),
                util._N(BPMSalesOrderItemFullfilment.GW_XOR_DROP_SHIPMENT),
                util._N(BPMSalesOrderItemFullfilment.EVENT_MSG_DROPSHIPMENT_CANCELLATION_RECEIVED),
                util._N(BPMSalesOrderItemFullfilment.GW_XOR_TRACKING_ID_RECEIVED),
                util._N(BPMSalesOrderItemFullfilment.EVENT_TRACKING_ID_RECEIVED),
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_CANCELLATION_DROPSHIPMENT),
                util._N(BPMSalesOrderItemFullfilment.ACTIVITY_HANDLE_CANCELLATION_DROPSHIPMENT),
                util._N(BPMSalesOrderItemFullfilment.EVENT_ORDER_CANCEL),
                util._N(BPMSalesOrderItemFullfilment.EVENT_ORDER_ITEM_CANCELLED),
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_HANDLE_ORDER_ITEM_CANCELLATION)
        );
        assertThat(firstItemProcessInstance).isEnded();

        // move remaining items
        util.sendMessage(ItemMessages.MSG_TRACKING_ID_RECEIVED, orderId);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderId);
        assertThat(salesOrderProcessInstance).isEnded();
    }

    @Test
    public void salesOrderItemShipmentCancellationPossibleTest() {
        final String orderId = util.getRandomOrderNumber();
        final String firstItem = orderId + "-item-" + 0;

        final ProcessInstance salesOrderProcessInstance = firstPartOfSalesOrderProcess(orderId);

        List<MessageCorrelationResult> msg_packingStarted = util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderId);

        final ProcessInstance firstItemProcessInstance = getFirstOrderItem(firstItem, msg_packingStarted);

        assertThat(firstItemProcessInstance).hasPassedInOrder(util._N(BPMSalesOrderItemFullfilment.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS));

        // cancel 1st orderItem
        final Map<String, Object> processVariables = Map.of(VAR_ITEM_CANCELLATION_POSSIBLE.getName(), true);
        util.sendMessage(ItemMessages.MSG_ORDER_ITEM_CANCELLATION_RECEIVED, orderId, firstItem, processVariables);

        // main process should stay at the same pos
        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Activities.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS));

        assertThat(firstItemProcessInstance).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(BPMSalesOrderItemFullfilment.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(BPMSalesOrderItemFullfilment.GW_XOR_SHIPMENT_METHOD),
                util._N(BPMSalesOrderItemFullfilment.EVENT_PACKING_STARTED),
                util._N(BPMSalesOrderItemFullfilment.GW_XOR_DROP_SHIPMENT),
                util._N(BPMSalesOrderItemFullfilment.EVENT_MSG_SHIPMENT_CANCELLATION_RECEIVED),
                util._N(BPMSalesOrderItemFullfilment.ACTIVITY_CHECK_CANCELLATION_POSSIBLE),
                util._N(BPMSalesOrderItemFullfilment.GW_XOR_CANCELLATION_POSSIBLE),
                util._N(BPMSalesOrderItemFullfilment.ACTIVITY_HANDLE_CANCELLATION_SHIPMENT),
                util._N(BPMSalesOrderItemFullfilment.EVENT_ORDER_ITEM_SHIPMENT_CANCELLED)
        );
        assertThat(firstItemProcessInstance).isEnded();

        // move remaining items
        util.sendMessage(ItemMessages.MSG_TRACKING_ID_RECEIVED, orderId);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderId);
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
                        util._N(Variables.VAR_ORDER_ID),
                        util._N(Variables.VAR_SHIPMENT_METHOD),
                        util._N(ItemVariables.VAR_ITEM_ID)
                );

        return firstItemProcessInstance;
    }

    protected ProcessInstance firstPartOfSalesOrderProcess(final String orderId) {
        final List<String> orderItems = getOrderItems(orderId, 5);
        final ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(Messages.MSG_ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderId)
                        .setVariable(util._N(Variables.VAR_ORDER_ID), orderId)
                        .setVariable(util._N(Variables.VAR_PAYMENT_TYPE), PAYMENT_CREDIT_CARD.getName())
                        .setVariable(util._N(Variables.VAR_ORDER_VALID), true)
                        .setVariable(util._N(Variables.VAR_ORDER_ITEMS), orderItems)
                        .setVariable(util._N(Variables.VAR_SHIPMENT_METHOD), PARCEL.getName())
                        .correlateWithResult().getProcessInstance();
        assertThat(salesOrderProcessInstance).isActive();
        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Activities.EVENT_MSG_ORDER_PAYMENT_SECURED));
        assertThat(salesOrderProcessInstance).hasPassed(util._N(Activities.ACTIVITY_VALIDATE_ORDER));

        runtimeService.createMessageCorrelation(util._N(Messages.MSG_ORDER_RECEIVED_PAYMENT_SECURED))
                .processInstanceBusinessKey(orderId)
                .setVariable(util._N(Variables.VAR_PAYMENT_STATUS), "captured")
                .correlateWithResult().getProcessInstance();

        assertThat(salesOrderProcessInstance).hasPassed(
                util._N(Activities.EVENT_CATCH_MSG_ORDER_CREATED),
                util._N(Activities.EVENT_THROW_MSG_ORDER_VALIDATED)
        );

        assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Activities.ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS));

        // start the subprocess
        // move all items to packing started
        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED, orderId);
        return salesOrderProcessInstance;
    }

    final List<String> getOrderItems(final String orderId, final int number) {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            result.add(orderId + "-item-" + i);
        }
        return result;
    }

    final ProcessInstanceQuery getProcessInstanceQuery() {
        return runtimeService.createProcessInstanceQuery();
    }

}
