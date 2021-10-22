package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
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
class CheckOrderTypeDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    CheckOrderTypeDelegate checkOrderTypeDelegate;

    @Test
    @SneakyThrows(Exception.class)
    public void checkOrderTypeForGarageOrder() {
        //Prepare the data
        doReturn("TestbranchTest").when(delegateExecution).getVariable(eq("salesChannel"));

        //Execute
        checkOrderTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("isBranchOrder"), eq(true));
    }

    @Test
    @SneakyThrows(Exception.class)
    public void checkOrderTypeForNonGarageOrder() {
        //Prepare the data
        doReturn("TestTestTest").when(delegateExecution).getVariable(Variables.SALES_CHANNEL.getName());

        //Execute
        checkOrderTypeDelegate.notify(delegateExecution);

        //Check the result
        verify(delegateExecution).setVariable(eq("isBranchOrder"), eq(false));
    }

}