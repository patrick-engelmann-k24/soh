package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Log
public class OrderCompletedDelegate implements JavaDelegate {

    private final SnsPublishService snsPublishService;
    private final AwsSnsConfig config;

    public OrderCompletedDelegate(SnsPublishService snsPublishService, AwsSnsConfig config) {
        this.snsPublishService = snsPublishService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + config.getSnsOrderCompletedTopic());

        String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        snsPublishService.sendOrder(config.getSnsOrderCompletedTopic(), "Sales Order Completed", orderNumber);
    }

}
