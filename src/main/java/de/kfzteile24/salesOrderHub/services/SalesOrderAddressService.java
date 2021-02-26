package de.kfzteile24.salesOrderHub.services;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemEvents;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;

@Service
@Log
public class SalesOrderAddressService {

    @Autowired
    CamundaHelper helper;

    @Autowired
    private SalesOrderRepository orderRepository;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private Gson gson;

    @SneakyThrows
    public ResponseEntity<String> updateBillingAddress(final String orderNumber, final Address newBillingAddress) {
        final Optional<SalesOrder> soOpt = orderRepository.getOrderByOrderNumber(orderNumber);
        if (soOpt.isPresent()) {
            if (helper.checkIfProcessExists(orderNumber)) {
                sendMessageForUpdateBillingAddress(orderNumber, newBillingAddress);
                final var newOrder = orderRepository.getOrderByOrderNumber(orderNumber);
                if (newOrder.isPresent()) {
                    SalesOrder updatedOrder = newOrder.get();
                    if (updatedOrder.getOriginalOrder().getOrderHeader().getBillingAddress().equals(newBillingAddress)) {
                        return new ResponseEntity<>("", HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>("Not possible to update invoice", HttpStatus.CONFLICT);
                    }
                }
            } else {
                return new ResponseEntity<>("Order not found", HttpStatus.NOT_FOUND);
            }
        }

        return ResponseEntity.notFound().build();
    }

    @SneakyThrows
    public ResponseEntity<String> updateDeliveryAddress(final String orderNumber, final String orderItemId, final Address newDeliveryAddress) {
        final Optional<SalesOrder> soOpt = orderRepository.getOrderByOrderNumber(orderNumber);

        if (soOpt.isPresent()) {
            if (helper.checkIfItemProcessExists(orderNumber, orderItemId)) {
                sendMessageForUpdateDeliveryAddress(
                        ItemMessages.DELIVERY_ADDRESS_CHANGE,
                        orderNumber, orderItemId, newDeliveryAddress);
                final Optional<SalesOrder> newOrder = orderRepository.getOrderByOrderNumber(orderNumber);
                if (newOrder.isPresent()) {
                    SalesOrder updatedOrder = newOrder.get();
                    List<Address> shippingAddresses = updatedOrder.getOriginalOrder().getOrderHeader().getShippingAddresses();
                    for(Address shippingAddress : shippingAddresses) {
                        if (shippingAddress.equals(newDeliveryAddress)) {
                            return new ResponseEntity<>("", HttpStatus.OK);
                        }
                    }

                    return new ResponseEntity<>("Update not possible ", HttpStatus.CONFLICT);
                }

            } else if (helper.checkIfProcessExists(orderNumber)) {
                // todo change main delivery address (order lvl)

            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        }
        return ResponseEntity.notFound().build();
    }

//    protected Execution tryUpdateBillingAddress(String orderNumber, Address newBillingAddress) {
//        final MessageCorrelationResult result = sendMessage(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED, orderNumber, newBillingAddress);
//
//        if (result.getExecution() != null) {
//            return result.getExecution();
//        } else {
//            return null;
//        }
//    }

    protected boolean tryUpdateDeliveryAddress(String orderNumber, String orderItemId, Address newDeliveryAddress) {
        final MessageCorrelationResult result = sendMessageForUpdateDeliveryAddress(ItemMessages.DELIVERY_ADDRESS_CHANGE, orderNumber, orderItemId, newDeliveryAddress);

        return helper.getProcessStatus(result.getExecution());
    }

    protected MessageCorrelationResult sendMessageForUpdateDeliveryAddress(BpmItem message, String orderNumber, String orderItemId, Address newDeliveryAddress) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(ItemVariables.ORDER_ITEM_ID.getName(), orderItemId)
                .setVariable(ItemVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName(), gson.toJson(newDeliveryAddress));

        return builder.correlateWithResultAndVariables(true);
    }

    protected MessageCorrelationResult sendMessageForUpdateBillingAddress(String orderNumber, Address newBillingAddress) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .setVariable(Variables.INVOICE_ADDRESS_CHANGE_REQUEST.getName(), gson.toJson(newBillingAddress));

        return builder.correlateWithResultAndVariables(true);
    }

}
