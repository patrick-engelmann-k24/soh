package de.kfzteile24.salesOrderHub.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqsReceiveService {

    final Gson gson;
    final RuntimeService runtimeService;
    final SalesOrderService salesOrderService;
    final CamundaHelper camundaHelper;

    @SqsListener("${soh.sqs.queue.ecpShopOrders}")
    public void queueListenerEcpShopOrders(String message, @Header("SenderId") String senderId) {
        log.info("message received: " + senderId);

        try {
            OrderJSON orderJSON = gson.fromJson(message, OrderJSON.class);
            final SalesOrder ecpSalesOrder = de.kfzteile24.salesOrderHub.domain.SalesOrder.builder()
                    .orderNumber(orderJSON.getOrderHeader().getOrderNumber())
                    .salesLocale(orderJSON.getOrderHeader().getOrigin().getLocale())
                    .originalOrder(orderJSON)
                    .build();

            salesOrderService.save(ecpSalesOrder);

            ProcessInstance result = camundaHelper.createOrderProcess(ecpSalesOrder, Messages.ORDER_RECEIVED_ECP);

            if (result != null) {
                log.info("New ecp order process started: " + result.getId());
            }
        } catch (JsonSyntaxException e) {
            log.error("ECP Order could not parsed from Json");
            log.error(e.getMessage());
        }
    }
}
