package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log
public class OrderItemCancelledDelegate implements JavaDelegate {


    @Autowired
    SnsPublishService snsPublishService;

    @Autowired
    SalesOrderService salesOrderService;

    @Autowired
    AwsSnsConfig config;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + config.getSnsOrderItemCancelledTopic());

        String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        snsPublishService.sendOrder(config.getSnsOrderItemCancelledTopic(), "Sales Order Item Cancelled", orderNumber);
    }

}
