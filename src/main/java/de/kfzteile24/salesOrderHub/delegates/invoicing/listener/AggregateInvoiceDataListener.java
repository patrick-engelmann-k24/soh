package de.kfzteile24.salesOrderHub.delegates.invoicing.listener;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_NUMBER_LIST;

@Component
@RequiredArgsConstructor
public class AggregateInvoiceDataListener implements ExecutionListener {

    @Override
    @Transactional
    public void notify(DelegateExecution delegateExecution) throws Exception {
        delegateExecution.setVariable(INVOICE_NUMBER_LIST.getName(), new ArrayList<>());
    }
}