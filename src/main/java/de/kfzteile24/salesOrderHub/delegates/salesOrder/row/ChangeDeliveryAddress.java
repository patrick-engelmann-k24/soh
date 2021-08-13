package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.Address;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.DELIVERY_ADDRESS_CHANGED;
import static java.text.MessageFormat.format;

@Component
@RequiredArgsConstructor
public class ChangeDeliveryAddress implements JavaDelegate {

    private final ObjectMapper objectMapper;
    private final SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        var newAddressStr = (String)execution.getVariable(RowVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName());
        var orderNumber = (String)execution.getVariable(Variables.ORDER_NUMBER.getName());
        var sku = (String)execution.getVariable(RowVariables.ORDER_ROW_ID.getName());

        final Address address = objectMapper.readValue(newAddressStr, Address.class);

        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        var latestJson = salesOrder.getLatestJson();
        final List<Address> addressList = latestJson.getOrderHeader().getShippingAddresses();
        if (!addressList.contains(address)) {
            int addressKey = addressList.size() + 1;
            address.setAddressKey(addressKey);
            addressList.add(address);
        }

        final Address foundAddress = addressList.get(addressList.indexOf(address));

        final OrderRows orderRow = latestJson.getOrderRows().stream()
                .filter(row -> sku.equals(row.getSku()))
                .findAny()
                .orElseThrow(() -> new NotFoundException(
                        format("Could not change delivery address: The sku {0} is not part of order {1}",
                                sku, orderNumber)));

        orderRow.setShippingAddressKey(foundAddress.getAddressKey());

        salesOrderService.save(salesOrder, DELIVERY_ADDRESS_CHANGED);
    }
}
