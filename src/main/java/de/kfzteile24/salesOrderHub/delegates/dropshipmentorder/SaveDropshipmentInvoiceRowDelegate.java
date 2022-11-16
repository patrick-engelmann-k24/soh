package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;

@Component
@Slf4j
@RequiredArgsConstructor
public class SaveDropshipmentInvoiceRowDelegate implements JavaDelegate {

    @NonNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @NonNull
    private final DropshipmentHelper dropshipmentHelper;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(ORDER_NUMBER.getName());
        final var sku = (String) delegateExecution.getVariable(ORDER_ROW.getName());
        log.info("Create Invoice tmp Entry based on sku {} and orderNumber {} in shipment tracking", sku, orderNumber);
        final var dropshipmentInvoiceRow = dropshipmentHelper.createDropshipmentInvoiceRow(sku, orderNumber);
        dropshipmentInvoiceRowService.save(dropshipmentInvoiceRow);
    }

}
