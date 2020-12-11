package de.kfzteile24.salesOrderHub.services;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Log
public class SalesOrderItemService {

    @Autowired
    CamundaHelper helper;

    @Autowired
    private SalesOrderRepository orderRepository;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private Gson gson;

    @SneakyThrows
    public Address changeDeliveryAddress(String orderNumber, String orderItemId, Address newDeliveryAddress) {
        final Optional<SalesOrder> soOpt = orderRepository.getOrderByOrderNumber(orderNumber);

        if (soOpt.isPresent()) {
            if (this.tryToChangeDeliveryAddress(orderNumber, orderItemId, newDeliveryAddress)) {
                final Optional<SalesOrder> newOrder = orderRepository.getOrderByOrderNumber(orderNumber);
                if (newOrder.isPresent()) {
                    return newOrder.get().getOriginalOrder().getOrderHeader().getShippingAddress();
                }
            }
        }
        return null;
    }

    protected boolean tryToChangeDeliveryAddress(String orderNumber, String orderItemId, Address newDeliveryAddress) {
        final MessageCorrelationResult result = sendMessage(ItemMessages.DELIVERY_ADDRESS_CHANGE, orderNumber, orderItemId, newDeliveryAddress);

        return getProcessStatus(result.getExecution());
    }

    protected MessageCorrelationResult sendMessage(BpmItem message, String orderNumber, String orderItemId, Address newDeliveryAdress) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(ItemVariables.ORDER_ITEM_ID.getName(), orderItemId)
                .setVariable(ItemVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName(), gson.toJson(newDeliveryAdress));

        return builder.correlateWithResult();
    }

    protected Boolean getProcessStatus(Execution execution) {
        return (Boolean) runtimeService.getVariable(execution.getId(), ItemVariables.DELIVERY_ADDRESS_CHANGE_POSSIBLE.getName());
    }

}
