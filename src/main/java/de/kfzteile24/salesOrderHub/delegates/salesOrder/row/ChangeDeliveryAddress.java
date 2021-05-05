package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.dto.order.LogisticalUnits;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.dto.order.logisticalUnits.Item;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.DELIVERY_ADDRESS_CHANGED;

@Component
@RequiredArgsConstructor
public class ChangeDeliveryAddress implements JavaDelegate {

    private final ObjectMapper objectMapper;
    private final SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        var newAddressStr = (String)execution.getVariable(RowVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName());
        var orderNumber = (String)execution.getVariable(Variables.ORDER_NUMBER.getName());
        var orderItemId = (String)execution.getVariable(RowVariables.ORDER_ROW_ID.getName());

        final Address address = objectMapper.readValue(newAddressStr, Address.class);

        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        final List<Address> addressList = salesOrder.getLatestJson().getOrderHeader().getShippingAddresses();
        if (!addressList.contains(address)) {
            int addressKey = addressList.size() + 1;
            address.setAddressKey(Integer.toString(addressKey));
            addressList.add(address);
        }

        final Address foundAddress = addressList.get(addressList.indexOf(address));

        final List<LogisticalUnits> logisticalUnits = salesOrder.getLatestJson().getLogisticalUnits();

        salesOrder.getLatestJson().getOrderRows().forEach(item -> {
            if (orderItemId.equals(item.getRowKey())) {
                final String rowKey = item.getRowKey();
                // find logistical unit
                logisticalUnits.forEach(unit -> {
                    List<Item> itemList = unit.getLogisticalItems();
                    itemList.forEach(logisticalItem -> {
                        if (logisticalItem.getRowKey().equals(rowKey)) {
                            unit.setShippingAddressKey(foundAddress.getAddressKey());
                        }
                    });
                });
            }
        });

        salesOrderService.save(salesOrder, DELIVERY_ADDRESS_CHANGED);
    }
}
