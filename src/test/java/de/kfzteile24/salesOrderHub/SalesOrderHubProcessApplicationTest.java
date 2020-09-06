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

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.MessageCorrelationResultWithVariables;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class SalesOrderHubProcessApplicationTest {

    final static String msgSalesOrder = "msg_orderReceivedMarketplace";
    public static final String MSG_ITEM_TRANSMITTED = "msg_itemTransmitted";
    public static final String MSG_ORDER_PAYMENT_SECURED = "msg_orderPaymentSecured";
    public static final String MSG_DROPSHIPMENT_CANCELLATION_RECEIVED = "msg_dropshipmentCancellationReceived";

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

    @Test
    public void salesOrderNotValidTest () {
        final String orderId = getOrderNumber();

        MessageCorrelationResult salesOrder = runtimeService.createMessageCorrelation(msgSalesOrder)
                .processInstanceBusinessKey(orderId)
                .setVariable("orderId", orderId)
                .setVariable("orderValid", false)
                .correlateWithResult();
    }

    @Test
    public void salesOrderItemAbortTest() {
        final String orderId = getOrderNumber();
        final String firstItem = orderId + "-item-" + 0;
        final List<String> orderItems = getOrderItems(orderId, 5);
        MessageCorrelationResult salesOrderProcess = runtimeService.createMessageCorrelation("msg_orderReceivedMarketplace")
                .processInstanceBusinessKey(orderId)
                .setVariable("orderId", orderId)
                .setVariable("payment_type", "creditCard")
                .setVariable("orderValid", true)
                .setVariable("orderItems", orderItems)
                .setVariable("shipment_method", "parcel")
                .correlateWithResult();
        assertNotNull(salesOrderProcess);

        runtimeService.createMessageCorrelation(MSG_ORDER_PAYMENT_SECURED)
                .processInstanceBusinessKey(orderId)
                .setVariable("payment_status", "captured")
                .correlate();

        runtimeService.createMessageCorrelation(MSG_ITEM_TRANSMITTED)
                .processInstanceVariableEquals("orderId", orderId)
                .processInstanceVariableEquals("orderItemId", firstItem)
                .correlateAll();

        List<MessageCorrelationResult> msg_packingStarted = runtimeService.createMessageCorrelation("msg_packingStarted")
                .processInstanceVariableEquals("orderId", orderId)
                .processInstanceVariableEquals("orderItemId", firstItem)
                .correlateAllWithResult();

        // cancel 1st orderItem
        runtimeService.createMessageCorrelation(MSG_DROPSHIPMENT_CANCELLATION_RECEIVED)
                .processInstanceVariableEquals("orderItemId", firstItem)
                .correlate();
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

}
