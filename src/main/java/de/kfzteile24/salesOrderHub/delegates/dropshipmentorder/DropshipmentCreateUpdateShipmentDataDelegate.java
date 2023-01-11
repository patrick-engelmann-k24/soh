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

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ITEMS_FULLY_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.QUANTITY_SHIPPED;

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
        final var quantityShipped = (Integer) delegateExecution.getVariable(QUANTITY_SHIPPED.getName());
        log.info("Update Dropshipment Shipment Confirmed Quantity information " +
                "for orderNumber {}, sku {} and quantity {}", orderNumber, sku, quantityShipped);

        var dropshipmentOrderRow = dropshipmentOrderRowService.addQuantityShipped(
                sku,
                orderNumber,
                quantityShipped);

        var quantityToBeInvoiced = calculateQuantityToBeInvoiced(quantityShipped, dropshipmentOrderRow);
        dropshipmentInvoiceRowService.create(sku, orderNumber, quantityToBeInvoiced);

        var itemsFullyShipped = dropshipmentOrderRowService.isItemsFullyShipped(orderNumber);
        delegateExecution.setVariable(ITEMS_FULLY_SHIPPED.getName(), itemsFullyShipped);

        log.info("Dropshipment Shipment Confirmed Quantity information is updated " +
                "for orderNumber {}. Items are fully shipped: {}", orderNumber, itemsFullyShipped);
    }

    public static Integer calculateQuantityToBeInvoiced(Integer quantityShipped, DropshipmentOrderRow dropshipmentOrderRow) {
        var totalQuantityShipped = dropshipmentOrderRow.getQuantityShipped();

        if (totalQuantityShipped > dropshipmentOrderRow.getQuantity()) {
            log.error("Total shipped quantity is bigger than the total quantity of the order row " +
                            "for orderNumber {}. Total quantity shipped : {} > total quantity of order row : {}",
                    dropshipmentOrderRow.getOrderNumber(), totalQuantityShipped, dropshipmentOrderRow.getQuantity());

            var difference = totalQuantityShipped - dropshipmentOrderRow.getQuantity();
            return quantityShipped - difference;
        }
        return quantityShipped;
    }
}
