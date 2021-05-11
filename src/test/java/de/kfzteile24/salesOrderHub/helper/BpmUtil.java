package de.kfzteile24.salesOrderHub.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;

@Component
public class BpmUtil {

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    ObjectMapper objectMapper;

    public final List<MessageCorrelationResult> sendMessage(BpmItem message, String orderNumber) {
        return this.sendMessage(_N(message), orderNumber);
    }

    public final List<MessageCorrelationResult> sendMessage(String message, String orderNumber) {
        return runtimeService.createMessageCorrelation(message)
                .processInstanceVariableEquals(_N(Variables.ORDER_NUMBER), orderNumber)
                .correlateAllWithResult();
    }

    public final List<MessageCorrelationResult> sendMessage(BpmItem message) {
        return this.sendMessage(_N(message));
    }

    public final List<MessageCorrelationResult> sendMessage(String message) {
        return runtimeService.createMessageCorrelation(message)
                .correlateAllWithResult();
    }

    public final MessageCorrelationResult sendMessage(final String message, final String orderNumber, final String orderItem) {
        return sendMessage(message, orderNumber, orderItem, Collections.emptyMap());
    }

    public final MessageCorrelationResult sendMessage(final BpmItem message, final String orderNumber, final String orderItem) {
        return sendMessage(_N(message), orderNumber, orderItem, Collections.emptyMap());
    }

    public final MessageCorrelationResult sendMessage(final String message, final String orderNumber, final String orderItem,
                                                      final Map<String, Object> processVariables) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message)
                .processInstanceVariableEquals(_N(Variables.ORDER_NUMBER), orderNumber)
                .processInstanceVariableEquals(_N(RowVariables.ORDER_ROW_ID), orderItem);
        if (!processVariables.isEmpty())
            builder.setVariables(processVariables);

        return builder
                .correlateWithResult();
    }

    public final MessageCorrelationResult sendMessage(final String message, final String orderNumber,
                                                      final Map<String, Object> processVariables) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message)
                .processInstanceVariableEquals(_N(Variables.ORDER_NUMBER), orderNumber);
        if (!processVariables.isEmpty())
            builder.setVariables(processVariables);

        return builder
                .correlateWithResult();
    }


    public final MessageCorrelationResult sendMessage(final BpmItem message, final String orderNumber, final String orderItem,
                                                      final Map<String, Object> processVariables) {
        return sendMessage(_N(message), orderNumber, orderItem, processVariables);
    }

    public final String getRandomOrderNumber() {
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

    @SneakyThrows(IOException.class)
    public final OrderJSON getRandomOrder() {
        final SqsMessage sqsMessage = objectMapper.readValue(loadOrderJson(), SqsMessage.class);
        final OrderJSON orderJSON = objectMapper.readValue(sqsMessage.getBody(), OrderJSON.class);

        orderJSON.getOrderHeader().setOrderNumber(getRandomOrderNumber());
        return orderJSON;
    }

    public final List<String> getOrderRows(final String orderNumber, final int number) {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            result.add(orderNumber + "-row-" + i);
        }
        return result;
    }

    public final String _N(BpmItem item) {
        return item.getName();
    }

    @SneakyThrows
    protected FileReader loadOrderJson() {
        String fileName = "examples/testmessage.json";
        return new FileReader(getClass().getResource(fileName).getFile());
    }

   public void finishOrderProcess(final ProcessInstance orderProcess, final String orderNumber) {
        // start subprocess
        sendMessage(_N(Messages.ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);

        // send items thru
        sendMessage(_N(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS), orderNumber);
        sendMessage(_N(RowMessages.PACKING_STARTED), orderNumber);
        sendMessage(_N(RowMessages.TRACKING_ID_RECEIVED), orderNumber);
        sendMessage(_N(RowMessages.ROW_SHIPPED), orderNumber);

       assertThat(orderProcess).isEnded().hasPassed(_N(Events.END_MSG_ORDER_COMPLETED));
    }

}
