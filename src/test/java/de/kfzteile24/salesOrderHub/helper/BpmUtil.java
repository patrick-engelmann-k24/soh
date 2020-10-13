package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BpmUtil {

    @Autowired
    RuntimeService runtimeService;

    public final List<MessageCorrelationResult> sendMessage(BpmItem message, String orderId) {
        return this.sendMessage(_N(message), orderId);
    }

    public final List<MessageCorrelationResult> sendMessage(String message, String orderId) {
        return runtimeService.createMessageCorrelation(message)
                .processInstanceVariableEquals("orderId", orderId)
                .correlateAllWithResult();
    }

    public final List<MessageCorrelationResult> sendMessage(BpmItem message) {
        return this.sendMessage(_N(message));
    }

    public final List<MessageCorrelationResult> sendMessage(String message) {
        return runtimeService.createMessageCorrelation(message)
                .correlateAllWithResult();
    }

    public final MessageCorrelationResult sendMessage(final String message, final String orderId, final String orderItem) {
        return sendMessage(message, orderId, orderItem, Collections.emptyMap());
    }

    public final MessageCorrelationResult sendMessage(final BpmItem message, final String orderId, final String orderItem) {
        return sendMessage(_N(message), orderId, orderItem, Collections.emptyMap());
    }

    public final MessageCorrelationResult sendMessage(final String message, final String orderId, final String orderItem,
                                                      final Map<String, Object> processVariables) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message)
                .processInstanceVariableEquals("orderId", orderId)
                .processInstanceVariableEquals("orderItemId", orderItem);
        if (!processVariables.isEmpty())
            builder.setVariables(processVariables);

        return builder
                .correlateWithResult();
    }

    public final MessageCorrelationResult sendMessage(final BpmItem message, final String orderId, final String orderItem,
                                                      final Map<String, Object> processVariables) {
        return sendMessage(_N(message), orderId, orderItem, processVariables);
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

    public final List<String> getOrderItems(final String orderId, final int number) {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            result.add(orderId + "-item-" + i);
        }
        return result;
    }

    public final String _N(BpmItem item) {
        return item.getName();
    }

}
