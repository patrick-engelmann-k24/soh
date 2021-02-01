package de.kfzteile24.salesOrderHub.services.task;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;

public class UpdateBillingAddress implements Runnable {

    private RuntimeService runtimeService;
    private Gson gson;

    final String orderNumber;
    final Address newAddress;

    public UpdateBillingAddress(final RuntimeService runtimeService, Gson gson, final String orderNumber, final Address newBillingAddress) {
        this.runtimeService = runtimeService;
        this.gson = gson;
        this.orderNumber = orderNumber;
        this.newAddress = newBillingAddress;
    }

    @SneakyThrows
    @Override
    public synchronized void run() {
        final MessageCorrelationResult result = sendMessage(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED, orderNumber, newAddress);

        final Execution execution = result.getExecution();

        int i = 0;
        while (!execution.isEnded()) {
            Thread.sleep(250);
            i++;

            System.out.println(i);

            if (i>= 25) {
                break;
            }
        }
    }

    protected MessageCorrelationResult sendMessage(BpmItem message, String orderNumber, Address newDeliveryAdress) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .setVariable(ItemVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName(), gson.toJson(newDeliveryAdress));

        return builder.correlateWithResultAndVariables(true);
    }

}
