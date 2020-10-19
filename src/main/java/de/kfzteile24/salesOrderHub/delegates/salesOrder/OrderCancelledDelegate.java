package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log
public class OrderCancelledDelegate implements JavaDelegate {

    @Autowired
    SnsPublishService snsPublishService;

    @Value("${soh.sns.topic.orderCancelled}")
    String snsOrderCancelledTopic;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + snsOrderCancelledTopic);

        String orderNumber = (String) delegateExecution.getVariable(Variables.VAR_ORDER_NUMBER.getName());
        snsPublishService.sendOrder(snsOrderCancelledTopic, "Sales Order Cancelled", orderNumber);
    }

}
