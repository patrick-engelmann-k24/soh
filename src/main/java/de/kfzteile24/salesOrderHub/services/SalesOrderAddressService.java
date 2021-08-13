package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.Address;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SalesOrderAddressService {

    private final CamundaHelper helper;

    private final SalesOrderRepository orderRepository;

    private final RuntimeService runtimeService;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public ResponseEntity<String> updateBillingAddress(final String orderNumber, final Address newBillingAddress) {
        final Optional<SalesOrder> soOpt = orderRepository.getOrderByOrderNumber(orderNumber);
        if (soOpt.isPresent()) {
            if (helper.checkIfActiveProcessExists(orderNumber)) {
                sendMessageForUpdateBillingAddress(orderNumber, newBillingAddress);
                final var newOrder = orderRepository.getOrderByOrderNumber(orderNumber);
                if (newOrder.isPresent()) {
                    final var latestJson = (Order) newOrder.get().getLatestJson();
                    if (latestJson.getOrderHeader().getBillingAddress().equals(newBillingAddress)) {
                        return new ResponseEntity<>("", HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>("The order was found but we could not change the billing address, because the order has already a invoice.", HttpStatus.CONFLICT);
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
                        RowMessages.DELIVERY_ADDRESS_CHANGE,
                        orderNumber, orderItemId, newDeliveryAddress);
                final Optional<SalesOrder> newOrder = orderRepository.getOrderByOrderNumber(orderNumber);
                if (newOrder.isPresent()) {
                    final var latestJson = (Order) newOrder.get().getLatestJson();
                    List<Address> shippingAddresses = latestJson.getOrderHeader().getShippingAddresses();
                    for(Address shippingAddress : shippingAddresses) {
                        if (shippingAddress.equals(newDeliveryAddress)) {
                            return new ResponseEntity<>("", HttpStatus.OK);
                        }
                    }

                    return new ResponseEntity<>("The order was found but could not changed the deliver address, because the state was not good.", HttpStatus.CONFLICT);
                }

            } else if (helper.checkIfActiveProcessExists(orderNumber)) {
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
        final MessageCorrelationResult result = sendMessageForUpdateDeliveryAddress(RowMessages.DELIVERY_ADDRESS_CHANGE, orderNumber, orderItemId, newDeliveryAddress);

        return helper.getProcessStatus(result.getExecution());
    }

    @SneakyThrows(JsonProcessingException.class)
    protected MessageCorrelationResult sendMessageForUpdateDeliveryAddress(BpmItem message, String orderNumber, String orderItemId, Address newDeliveryAddress) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(RowVariables.ORDER_ROW_ID.getName(), orderItemId)
                .setVariable(RowVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName(),
                        objectMapper.writeValueAsString(newDeliveryAddress));

        return builder.correlateWithResultAndVariables(true);
    }

    @SneakyThrows(JsonProcessingException.class)
    protected MessageCorrelationResult sendMessageForUpdateBillingAddress(String orderNumber, Address newBillingAddress) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .setVariable(Variables.INVOICE_ADDRESS_CHANGE_REQUEST.getName(),
                        objectMapper.writeValueAsString(newBillingAddress));

        return builder.correlateWithResultAndVariables(true);
    }

}
