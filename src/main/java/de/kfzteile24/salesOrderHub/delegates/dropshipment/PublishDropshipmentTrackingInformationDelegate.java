package de.kfzteile24.salesOrderHub.delegates.dropshipment;

import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;

@Component
@Slf4j
@RequiredArgsConstructor
public class PublishDropshipmentTrackingInformationDelegate implements JavaDelegate {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final SnsPublishService snsPublishService;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(ORDER_NUMBER.getName());
        final var trackingLinks = (Collection<String>) delegateExecution.getVariable(TRACKING_LINKS.getName());
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        log.info("Publish Dropshipment Tracking Information process with order number {} is started", orderNumber);
        snsPublishService.publishSalesOrderShipmentConfirmedEvent(salesOrder, trackingLinks);
    }

}
