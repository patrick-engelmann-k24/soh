package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

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


    private final SnsPublishService snsPublishService;
    private final SalesOrderService salesOrderService;
    private final AwsSnsConfig config;

    public OrderItemCancelledDelegate(SnsPublishService snsPublishService, SalesOrderService salesOrderService, AwsSnsConfig config) {
        this.snsPublishService = snsPublishService;
        this.salesOrderService = salesOrderService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + config.getSnsOrderItemCancelledTopic());

        String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        snsPublishService.sendOrder(config.getSnsOrderItemCancelledTopic(), "Sales Order Item Cancelled", orderNumber);
    }

}
