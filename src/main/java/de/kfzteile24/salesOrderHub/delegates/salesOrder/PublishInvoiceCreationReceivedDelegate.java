package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishInvoiceCreationReceivedDelegate implements JavaDelegate {

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var salesOrderId = (UUID) delegateExecution.getVariable(Variables.SALES_ORDER_ID.getName());
        final var salesOrder = salesOrderService.getOrderById(salesOrderId)
                .orElseThrow(() -> new SalesOrderNotFoundCustomException("Could not find order with id: " + salesOrderId
                        + " for publishing core sales invoice event"));
        snsPublishService.publishCoreInvoiceReceivedEvent(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder.getInvoiceEvent()));
        log.info("{} delegate invoked", PublishInvoiceCreationReceivedDelegate.class.getSimpleName());
    }
}
