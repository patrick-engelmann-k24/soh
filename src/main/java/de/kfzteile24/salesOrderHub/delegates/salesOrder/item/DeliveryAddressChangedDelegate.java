package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log
public class DeliveryAddressChangedDelegate extends CommonDelegate {

    @Autowired
    SnsPublishService snsPublishService;

    @Value("${soh.sns.topic.deliveryAddressChanged}")
    String snsDeliveryAddressChanged;

    @Autowired
    SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + snsDeliveryAddressChanged);

        String orderNumber = (String) delegateExecution.getVariable(Variables.VAR_ORDER_NUMBER.getName());
        snsPublishService.sendOrder(snsDeliveryAddressChanged, "Sales Order Invoice Address Changed", orderNumber);
    }
}
