package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupDropshipmentInvoiceRowTableDelegate implements JavaDelegate {

    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        log.info("Cleaning up all temporary data in dropshipment invoice row table");
        dropshipmentInvoiceRowService.deleteAll();
    }
}
