package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log
public class DeliveryAddressChangedDelegate extends CommonDelegate {

    @Autowired
    SnsPublishService snsPublishService;

    @Autowired
    AwsSnsConfig config;

    @Autowired
    SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + config.getSnsDeliveryAddressChanged());

        String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        snsPublishService.sendOrder(config.getSnsDeliveryAddressChanged(), "Sales Order Invoice Address Changed", orderNumber);
    }
}
