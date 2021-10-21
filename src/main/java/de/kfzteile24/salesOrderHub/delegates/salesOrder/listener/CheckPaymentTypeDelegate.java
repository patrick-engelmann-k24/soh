package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class CheckPaymentTypeDelegate implements ExecutionListener {

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        //The following payment types do not need to wait for payment secured event.
        final String paymentType = (String) delegateExecution.getVariable(Variables.PAYMENT_TYPE.getName());
        boolean positivePaymentType =
                paymentType.equals(PaymentType.AMAZON.getName())
                || paymentType.equals(PaymentType.EBAY.getName())
                || paymentType.equals(PaymentType.CASH.getName())
                || paymentType.equals(PaymentType.CASH_ON_DELIVERY.getName())
                || paymentType.equals(PaymentType.B2B_CASH_ON_DELIVERY.getName());

        delegateExecution.setVariable(Variables.POSITIVE_PAYMENT_TYPE.getName(), positivePaymentType);
    }
}
