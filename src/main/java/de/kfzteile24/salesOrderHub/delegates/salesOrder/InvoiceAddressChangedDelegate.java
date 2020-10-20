package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
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
public class InvoiceAddressChangedDelegate implements JavaDelegate {

    @Autowired
    SnsPublishService snsPublishService;

    @Autowired
    AwsSnsConfig config;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + config.getSnsInvoiceAddressChangedTopic());

        String orderNumber = (String) delegateExecution.getVariable(Variables.VAR_ORDER_NUMBER.getName());
        snsPublishService.sendOrder(config.getSnsInvoiceAddressChangedTopic(), "Invoice Address Changed", orderNumber);
    }

}
