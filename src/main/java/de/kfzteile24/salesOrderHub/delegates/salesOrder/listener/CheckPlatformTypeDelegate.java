package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.soh.order.dto.Platform;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CheckPlatformTypeDelegate implements ExecutionListener {

    @Override
    public void notify(DelegateExecution delegateExecution) {
        final String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final String platformType = (String) delegateExecution.getVariable(Variables.PLATFORM_TYPE.getName());
        delegateExecution.setVariable(Variables.IS_SOH_ORDER.getName(), isSohOrder(orderNumber, platformType));
    }

    protected boolean isSohOrder(String orderNumber, String platformType) {
        boolean isSohOrder = false;
        if (platformType == null) {
            log.error("Unable to identify the platform type. Order Number: {}", orderNumber);
        } else {
            isSohOrder = platformType.equals(Platform.SOH.name());
        }
        log.info("The platform type check has the following result. " +
                        "Order Number: {}, PlatformType: {}, Is Soh Order?: {}",
                orderNumber, platformType, isSohOrder);
        return isSohOrder;
    }
}
