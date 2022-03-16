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
class CheckPlatformTypeDelegateTest {


    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    CheckPlatformTypeDelegate checkPlatformTypeDelegate;

    @Test
    @SneakyThrows(Exception.class)
    public void testSoh() {

        //Prepare the data
        doReturn("11111111111111").when(delegateExecution).getVariable(eq("orderNumber"));
        doReturn("SOH").when(delegateExecution).getVariable(eq("platformType"));

        //Execute
        checkPlatformTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("isSohOrder"), eq(true));
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testAnyOtherPlatformType() {
        //Prepare the data
        doReturn("11111111111111").when(delegateExecution).getVariable(eq("orderNumber"));
        doReturn("ECP").when(delegateExecution).getVariable(eq("platformType"));

        //Execute
        checkPlatformTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("isSohOrder"), eq(false));
    }
}