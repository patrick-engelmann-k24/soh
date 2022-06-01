package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.text.MessageFormat;

@Component
@Slf4j
@RequiredArgsConstructor
public class SaveCreditNoteDelegate implements JavaDelegate {

    private final SalesOrderReturnService salesOrderReturnService;

    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var returnOrderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var invoiceUrl = (String) delegateExecution.getVariable(Variables.INVOICE_URL.getName());

        SalesOrderReturn returnOrder = salesOrderReturnService.getByOrderNumber(returnOrderNumber);
        if (returnOrder == null) {
            throw new NotFoundException(MessageFormat.format("Return order not found for the given order number {0} ", returnOrderNumber));
        }
        returnOrder.setUrl(invoiceUrl);
        salesOrderReturnService.save(returnOrder);
    }
}
