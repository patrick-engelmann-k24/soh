package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChangeInvoiceAddressDelegate implements JavaDelegate {

    private final ObjectMapper objectMapper;

    private final SalesOrderRepository orderRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) throws JsonProcessingException {
        var newAddressStr = (String)delegateExecution.getVariable(Variables.INVOICE_ADDRESS_CHANGE_REQUEST.getName());
        var orderNumber = (String)delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final Address address = objectMapper.readValue(newAddressStr, Address.class);

        var salesOrderOptional = orderRepository.getOrderByOrderNumber(orderNumber);

        if (salesOrderOptional.isPresent()) {
            final SalesOrder salesOrder = salesOrderOptional.get();
            salesOrder.getLatestJson().getOrderHeader().setBillingAddress(address);
            orderRepository.save(salesOrder);
        }

    }
}
