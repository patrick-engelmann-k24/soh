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
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.BillingAddress;
import de.kfzteile24.soh.order.dto.ShippingAddress;
import lombok.NonNull;
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

import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_ADDRESS_CHANGED;

@Service
@RequiredArgsConstructor
public class SalesOrderAddressService {

    private final SalesOrderService salesOrderService;

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final CamundaHelper helper;

    @NonNull
    private final SalesOrderRepository orderRepository;

    @NonNull
    private final RuntimeService runtimeService;

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final TimedPollingService timerService;

    @NonNull
    private final InvoiceService invoiceService;

    @SneakyThrows
    public ResponseEntity<String> updateBillingAddress(final String orderNumber, final BillingAddress newBillingAddress) {
        final Optional<SalesOrder> soOpt = orderRepository.getOrderByOrderNumber(orderNumber);
        if (soOpt.isPresent()) {
            if (invoiceService.checkInvoiceExistsForOrder(orderNumber)) {
                return new ResponseEntity<>("The billing address cannot be changed anymore, because the order has already an invoice.", HttpStatus.CONFLICT);
            } else {

                var salesOrder = soOpt.get();

                salesOrder.getLatestJson().getOrderHeader().setBillingAddress(newBillingAddress);
                salesOrderService.save(salesOrder, INVOICE_ADDRESS_CHANGED);
                snsPublishService.publishInvoiceAddressChanged(orderNumber);
                return new ResponseEntity<>("", HttpStatus.OK);
            }
        }

        return ResponseEntity.notFound().build();
    }

    @SneakyThrows
    public ResponseEntity<String> updateDeliveryAddress(final String orderNumber, final String orderRowId, final ShippingAddress newDeliveryAddress) {
        final Optional<SalesOrder> soOpt = orderRepository.getOrderByOrderNumber(orderNumber);

        if (soOpt.isPresent()) {
            if (helper.checkIfOrderRowProcessExists(orderNumber, orderRowId)) {
                sendMessageForUpdateDeliveryAddress(
                        RowMessages.DELIVERY_ADDRESS_CHANGE,
                        orderNumber, orderRowId, newDeliveryAddress);

                final var addressChanged = timerService.pollWithDefaultTiming(() -> {
                    final var newOrder = orderRepository.getOrderByOrderNumber(orderNumber);
                    if (newOrder.isPresent()) {
                        final List<ShippingAddress> shippingAddresses =
                                newOrder.get().getLatestJson().getOrderHeader().getShippingAddresses();
                        for(ShippingAddress shippingAddress : shippingAddresses) {
                            if (shippingAddress.equals(newDeliveryAddress)) {
                                return true;
                            }
                        }
                    }
                    return false;
                });

                if (addressChanged) {
                    return new ResponseEntity<>("", HttpStatus.OK);
                } else {
                    return new ResponseEntity<>("The order was found but could not changed the deliver address, because the state was not good.", HttpStatus.CONFLICT);
                }
            //} else if (helper.checkIfActiveProcessExists(orderNumber)) {
                // todo change main delivery address (order lvl)

            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    protected boolean tryUpdateDeliveryAddress(String orderNumber, String orderItemId, ShippingAddress newDeliveryAddress) {
        final MessageCorrelationResult result = sendMessageForUpdateDeliveryAddress(RowMessages.DELIVERY_ADDRESS_CHANGE, orderNumber, orderItemId, newDeliveryAddress);

        return helper.getProcessStatus(result.getExecution());
    }

    @SneakyThrows(JsonProcessingException.class)
    protected MessageCorrelationResult sendMessageForUpdateDeliveryAddress(BpmItem message, String orderNumber, String orderItemId, ShippingAddress newDeliveryAddress) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(RowVariables.ORDER_ROW_ID.getName(), orderItemId)
                .setVariable(RowVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName(),
                        objectMapper.writeValueAsString(newDeliveryAddress));

        return builder.correlateWithResultAndVariables(true);
    }

    @SneakyThrows(JsonProcessingException.class)
    protected MessageCorrelationResult sendMessageForUpdateBillingAddress(String orderNumber, BillingAddress newBillingAddress) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .setVariable(Variables.INVOICE_ADDRESS_CHANGE_REQUEST.getName(),
                        objectMapper.writeValueAsString(newBillingAddress));

        return builder.correlateWithResultAndVariables(true);
    }

}
