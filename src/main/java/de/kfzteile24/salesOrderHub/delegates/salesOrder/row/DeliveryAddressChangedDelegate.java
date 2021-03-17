package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

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

    private final SnsPublishService snsPublishService;
    private final AwsSnsConfig config;
    private final SalesOrderService salesOrderService;

    public DeliveryAddressChangedDelegate(SnsPublishService snsPublishService,
                                          AwsSnsConfig config,
                                          SalesOrderService salesOrderService) {
        this.snsPublishService = snsPublishService;
        this.config = config;
        this.salesOrderService = salesOrderService;
    }

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + config.getSnsDeliveryAddressChanged());

        String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        snsPublishService.sendOrder(config.getSnsDeliveryAddressChanged(), "Sales Order Invoice Address Changed", orderNumber);
    }
}
