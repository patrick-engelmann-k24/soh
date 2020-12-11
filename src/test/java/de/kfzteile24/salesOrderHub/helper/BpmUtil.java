package de.kfzteile24.salesOrderHub.helper;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.sqs.EcpOrder;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Component
public class BpmUtil {

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    Gson gson;

    @Autowired
    @Qualifier("messageHeader")
    Gson gsonMessageHeader;

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
                .processInstanceVariableEquals(_N(ItemVariables.ORDER_ITEM_ID), orderItem);
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

    public final OrderJSON getRandomOrder() {
        final EcpOrder messageHeader = gsonMessageHeader.fromJson(loadOrderJson(), EcpOrder.class);
        final OrderJSON orderJSON = gson.fromJson(messageHeader.getMessage(), OrderJSON.class);

        orderJSON.getOrderHeader().setOrderNumber(getRandomOrderNumber());
        return orderJSON;
    }

    public final List<String> getOrderItems(final String orderNumber, final int number) {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            result.add(orderNumber + "-item-" + i);
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

}
