package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PublishDropshipmentItemShipmentCompletedDelegate implements JavaDelegate {

    private final CamundaHelper camundaHelper;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        log.info("Dropshipment Order Fully Shipped Delegate for order number {}", orderNumber);
        camundaHelper.correlateMessage(Messages.DROPSHIPMENT_ORDER_FULLY_COMPLETED, orderNumber);
    }
}
