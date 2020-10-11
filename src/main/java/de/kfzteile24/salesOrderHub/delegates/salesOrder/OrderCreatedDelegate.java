package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.BPMSalesOrderProcess;
import de.kfzteile24.salesOrderHub.constants.BpmItem;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Log
public class OrderCreatedDelegate implements JavaDelegate {

    @Autowired
    SnsPublishService snsPublishService;

    @Value("${soh.sns.topic.orderCreated}")
    String snsOrderCreatedTopic;

    @Autowired
    SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + snsOrderCreatedTopic);

        String orderNumber = (String) delegateExecution.getVariable(BPMSalesOrderProcess.VAR_ORDER_ID.getName());
        Optional<SalesOrder> salesOrderOptional = salesOrderService.getOrderByOrderNumber(orderNumber);
        if (salesOrderOptional.isPresent()) {
            SalesOrder salesOrder = salesOrderOptional.get();
            snsPublishService.send(snsOrderCreatedTopic, "Sales Order Created", salesOrder.getOriginalOrder());
        } else {
            throw new Exception("no sales order found");
        }
    }
}
