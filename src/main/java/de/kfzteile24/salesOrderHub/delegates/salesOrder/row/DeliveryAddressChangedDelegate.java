package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryAddressChangedDelegate extends CommonDelegate {

    @NonNull
    private final SnsPublishService snsPublishService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        snsPublishService.publishDeliveryAddressChanged(orderNumber);
    }
}
