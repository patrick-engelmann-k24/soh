package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log
public class OrderCreatedDelegate implements JavaDelegate {

    @Autowired
    SnsPublishService snsPublishService;

    @Value("${soh.sns.topic.orderCreated}")
    String snsOrderCreatedTopic; //= "arn:aws:sns:eu-central-1:000000000000:soh-order-created";

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("SNS-Topic: " + snsOrderCreatedTopic);
        snsPublishService.send(snsOrderCreatedTopic, "subject", "Body");
    }
}
