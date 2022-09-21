package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CheckPaymentTypeDelegate implements ExecutionListener {

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        //The following payment types do not need to wait for payment secured event.
        final String paymentType = (String) delegateExecution.getVariable(Variables.PAYMENT_TYPE.getName());
        boolean positivePaymentType = Optional.ofNullable(paymentType)
                .map(s -> s.equals(PaymentType.CASH_ON_DELIVERY.getName())
                        || s.equals(PaymentType.B2B_CASH_ON_DELIVERY.getName())
                        || s.equals(PaymentType.AFTERPAY_DEBIT.getName())
                        || s.equals(PaymentType.AFTERPAY_INVOICE.getName())
                        || s.equals(PaymentType.PAYMENT_IN_ADVANCE.getName())
                        || s.equals(PaymentType.B2B_INVOICE.getName())
                        || s.equals(PaymentType.AMAZON.getName())
                        || s.equals(PaymentType.EBAY.getName()))
                .orElse(false);

        delegateExecution.setVariable(Variables.POSITIVE_PAYMENT_TYPE.getName(), positivePaymentType);
    }
}
