package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesOrderService {

    @NonNull
    private final CamundaHelper helper;

    @NonNull
    private final SalesOrderRepository orderRepository;

    @NonNull
    private final AuditLogRepository auditLogRepository;

    @NonNull
    private final InvoiceService invoiceService;

    @NonNull
    private final RuntimeService runtimeService;

    public SalesOrder updateOrder(final SalesOrder salesOrder) {
        salesOrder.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(salesOrder);
    }

    public Boolean isOrderBillingAddressChangeable(String orderNumber) {
        final Optional<SalesOrder> order = this.getOrderByOrderNumber(orderNumber);
        if (order.isPresent()) {
            return invoiceService.checkInvoiceExistentForOrder(order.get().getOrderNumber());
        }

        return false;
    }

    public ResponseEntity<String> cancelOrder(String orderNumber) {
        final Optional<SalesOrder> orderOptional = this.getOrderByOrderNumber(orderNumber);
        if (orderOptional.isPresent()) {
            if (helper.checkIfActiveProcessExists(orderNumber)) {
                sendMessageForOrderCancellation(orderNumber);
                if (!helper.checkIfActiveProcessExists(orderNumber)) {
                    return ResponseEntity.ok().build();
                } else {
                    return new ResponseEntity<>("The order was found but could not cancelled, because the order rows are already in progress.", HttpStatus.CONFLICT);
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public SalesOrder createOrder(SalesOrder salesOrder) {
        return orderRepository.save(salesOrder);
    }

    public Optional<SalesOrder> findById(UUID id) {
        return orderRepository.findById(id);
    }

    public Optional<SalesOrder> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.getOrderByOrderNumber(orderNumber);
    }

    public Optional<SalesOrder> getOrderByProcessId(String processId) {
        return orderRepository.getOrderByProcessId(processId);
    }

    public SalesOrder save(SalesOrder order, Action action) {
        final var storedOrder = orderRepository.save(order);

        final var auditLog = AuditLog.builder()
                .salesOrderId(storedOrder.getId())
                .action(action)
                .data(order.getLatestJson())
                .build();

        auditLogRepository.save(auditLog);

        return storedOrder;
    }

    /**
     * checks if there is any order in the past for this customer. If yes then sets the status
     * of the order to recurring.
     * @param salesOrder
     */
    public boolean isRecurringOrder(SalesOrder salesOrder) {
        return orderRepository.countByCustomerEmail(salesOrder.getCustomerEmail()) > 0;
    }

    protected MessageCorrelationResult sendMessageForOrderCancellation(String orderNumber) {
        MessageCorrelationBuilder builder = runtimeService
                .createMessageCorrelation(Messages.ORDER_CANCELLATION_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                ;

        return builder.correlateWithResultAndVariables(true);
    }
}
