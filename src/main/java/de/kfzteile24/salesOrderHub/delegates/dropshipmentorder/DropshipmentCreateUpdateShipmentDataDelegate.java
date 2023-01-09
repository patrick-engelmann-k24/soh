package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderRowService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ITEM_FULLY_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW_QUANTITY;

@Component
@Slf4j
@RequiredArgsConstructor
public class DropshipmentCreateUpdateShipmentDataDelegate implements JavaDelegate {

    @NonNull
    private final DropshipmentOrderRowService dropshipmentOrderRowService;
    @NonNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(ORDER_NUMBER.getName());
        final var sku = (String) delegateExecution.getVariable(ORDER_ROW.getName());
        final var quantityShipped = (Integer) delegateExecution.getVariable(ORDER_ROW_QUANTITY.getName());
        log.info("Update Dropshipment Shipment Confirmed Quantity information " +
                "for orderNumber {}, sku {} and quantity {}", orderNumber, sku, quantityShipped);

        var dropshipmentOrderRow = dropshipmentOrderRowService.addQuantityShipped(
                sku,
                orderNumber,
                quantityShipped);

        dropshipmentInvoiceRowService.create(
                sku,
                orderNumber,
                getQuantityToBeInvoiced(quantityShipped, dropshipmentOrderRow));

        var itemsFullyShipped = dropshipmentOrderRowService.isItemsFullyShipped(orderNumber);
        delegateExecution.setVariable(ITEM_FULLY_SHIPPED.getName(), itemsFullyShipped);
        log.info("Dropshipment Shipment Confirmed Quantity information is updated " +
                "for orderNumber {}. Items are fully shipped: {}", orderNumber, itemsFullyShipped);
    }

    private static Integer getQuantityToBeInvoiced(Integer quantityShipped, DropshipmentOrderRow dropshipmentOrderRow) {
        var quantityToBeInvoiced = quantityShipped;
        if (quantityShipped > dropshipmentOrderRow.getQuantity()) {
            quantityToBeInvoiced = dropshipmentOrderRow.getQuantity();
        }
        return quantityToBeInvoiced;
    }
}
