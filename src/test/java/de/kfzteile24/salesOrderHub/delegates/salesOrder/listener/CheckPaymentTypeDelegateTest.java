package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CheckPaymentTypeDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    CheckPaymentTypeDelegate checkPaymentTypeDelegate;

    @Test
    @SneakyThrows(Exception.class)
    public void testCashOnDelivery() {
        //Prepare the data
        doReturn("cash_on_delivery").when(delegateExecution).getVariable(eq("paymentType"));

        //Execute
        checkPaymentTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("positivePaymentType"), eq(true));
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testB2BCashOnDelivery() {
        //Prepare the data
        doReturn("b2b_cash_on_delivery").when(delegateExecution).getVariable(eq("paymentType"));

        //Execute
        checkPaymentTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("positivePaymentType"), eq(true));
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testAfterPayDebit() {
        //Prepare the data
        doReturn("afterpay_debit").when(delegateExecution).getVariable(eq("paymentType"));

        //Execute
        checkPaymentTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("positivePaymentType"), eq(true));
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testAfterPayInvoice() {
        //Prepare the data
        doReturn("afterpay_invoice").when(delegateExecution).getVariable(eq("paymentType"));

        //Execute
        checkPaymentTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("positivePaymentType"), eq(true));
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testPaymentInAdvance() {
        //Prepare the data
        doReturn("payment_in_advance").when(delegateExecution).getVariable(eq("paymentType"));

        //Execute
        checkPaymentTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("positivePaymentType"), eq(true));
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testB2BInvoice() {
        //Prepare the data
        doReturn("business_to_business_invoice").when(delegateExecution).getVariable(eq("paymentType"));

        //Execute
        checkPaymentTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("positivePaymentType"), eq(true));
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testAmazon() {
        //Prepare the data
        doReturn("amazonmarketplace").when(delegateExecution).getVariable(eq("paymentType"));

        //Execute
        checkPaymentTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("positivePaymentType"), eq(true));
    }


    @Test
    @SneakyThrows(Exception.class)
    public void testEbay() {
        //Prepare the data
        doReturn("ebay_payment").when(delegateExecution).getVariable(eq("paymentType"));

        //Execute
        checkPaymentTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("positivePaymentType"), eq(true));
    }

    @Test
    @SneakyThrows(Exception.class)
    public void anyOtherPaymentTypeShouldWaitForPaymentSecured() {
        //Prepare the data
        doReturn("creditcard").when(delegateExecution).getVariable(eq("paymentType"));

        //Execute
        checkPaymentTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("positivePaymentType"), eq(false));
    }
}