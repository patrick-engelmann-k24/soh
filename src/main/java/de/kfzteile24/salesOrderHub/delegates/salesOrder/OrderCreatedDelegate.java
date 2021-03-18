package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log
public class OrderCreatedDelegate implements JavaDelegate {

    private final SnsPublishService snsPublishService;
    private final AwsSnsConfig config;

    public OrderCreatedDelegate(SnsPublishService snsPublishService, AwsSnsConfig config) {
        this.snsPublishService = snsPublishService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + config.getSnsOrderCreatedTopic());

        String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        snsPublishService.sendOrder(config.getSnsOrderCreatedTopic(), "Sales Order Created", orderNumber);
    }
}
