package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.Address;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_ADDRESS_CHANGED;

@Component
@RequiredArgsConstructor
public class ChangeInvoiceAddressDelegate implements JavaDelegate {

    private final ObjectMapper objectMapper;

    private final SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws JsonProcessingException {
        var newAddressStr = (String)delegateExecution.getVariable(Variables.INVOICE_ADDRESS_CHANGE_REQUEST.getName());
        var orderNumber = (String)delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final Address address = objectMapper.readValue(newAddressStr, Address.class);

        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        salesOrder.getLatestJson().getOrderHeader().setBillingAddress(address);
        salesOrderService.save(salesOrder, INVOICE_ADDRESS_CHANGED);
    }
}
