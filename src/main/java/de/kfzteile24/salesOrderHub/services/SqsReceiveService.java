package de.kfzteile24.salesOrderHub.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.sqs.EcpOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqsReceiveService {

    final Gson gson;
    final RuntimeService runtimeService;
    final SalesOrderService salesOrderService;

    @SqsListener("${soh.sqs.queue.ecpShopOrders}")
    public void queueListenerEcpShopOrders(String message, @Header("SenderId") String senderId) {
        log.info("message received: " + senderId);

        try {
            OrderJSON orderJSON = gson.fromJson(message, OrderJSON.class);
            final SalesOrder ecpSalesOrder = de.kfzteile24.salesOrderHub.domain.SalesOrder.builder()
                    .orderNumber(orderJSON.getOrderHeader().getOrderNumber())
                    .salesLocale(orderJSON.getOrderHeader().getOrigin().getLocale())
                    .originalOrder(gson.toJson(orderJSON, OrderJSON.class))
                    .build();

            salesOrderService.save(ecpSalesOrder);

            MessageCorrelationResult result = runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_ECP.getName())
                    .processInstanceBusinessKey(ecpSalesOrder.getOrderNumber())
                    .setVariable(Variables.ORDER_NUMBER.getName(), ecpSalesOrder.getOrderNumber())
                    .correlateWithResult();

            if (result.getProcessInstance() != null) {
                log.info("New ecp order process started: " + result.getProcessInstance().getId());
            }
        } catch (JsonSyntaxException e) {
            log.error("ECP Order could not parsed from Json");
            log.error(e.getMessage());
        }
    }
}
