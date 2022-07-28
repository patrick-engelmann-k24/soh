package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
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

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.CORE_INVOICE_PUBLISHED;
import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_INVOICE_PUBLISHED;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishCoreSalesInvoiceCreatedReceivedDelegate implements JavaDelegate {

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final MetricsHelper metricsHelper;

    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var salesOrderId = (UUID) delegateExecution.getVariable(Variables.SALES_ORDER_ID.getName());
        final var salesOrder = salesOrderService.getOrderById(salesOrderId)
                .orElseThrow(() -> new SalesOrderNotFoundCustomException("Could not find order with id: " + salesOrderId
                        + " for publishing core sales invoice event"));
        snsPublishService.publishCoreInvoiceReceivedEvent(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder.getInvoiceEvent()));
        log.info("{} delegate invoked", PublishCoreSalesInvoiceCreatedReceivedDelegate.class.getSimpleName());

        var orderFulfillment = salesOrder.getLatestJson().getOrderHeader().getOrderFulfillment();
        if (!equalsIgnoreCase(orderFulfillment, DELTICOM.getName())) {
            metricsHelper.sendCustomEventForInvoices(salesOrder, CORE_INVOICE_PUBLISHED);
        } else {
            metricsHelper.sendCustomEventForInvoices(salesOrder, DROPSHIPMENT_INVOICE_PUBLISHED);
        }
    }
}
