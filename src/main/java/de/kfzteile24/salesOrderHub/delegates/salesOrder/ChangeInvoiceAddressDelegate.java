package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log
public class ChangeInvoiceAddressDelegate implements JavaDelegate {

    @Autowired
    private Gson gson;

    @Autowired
    private SalesOrderRepository orderRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        var newAddressStr = (String)delegateExecution.getVariable(Variables.INVOICE_ADDRESS_CHANGE_REQUEST.getName());
        var orderNumber = (String)delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final Address address = gson.fromJson(newAddressStr, Address.class);

        var salesOrderOptional = orderRepository.getOrderByOrderNumber(orderNumber);

        if (salesOrderOptional.isPresent()) {
            final SalesOrder salesOrder = salesOrderOptional.get();
            salesOrder.getOriginalOrder().getOrderHeader().setBillingAddress(address);
            orderRepository.save(salesOrder);
        }

    }
}
