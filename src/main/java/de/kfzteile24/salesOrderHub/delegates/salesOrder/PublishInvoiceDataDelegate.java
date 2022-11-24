package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.CORE_INVOICE_PUBLISHED;
import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_INVOICE_PUBLISHED;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishInvoiceDataDelegate implements JavaDelegate {

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final MetricsHelper metricsHelper;

    @NonNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;


    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var invoiceNumber = (String) delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName());
        final var orderNumber = dropshipmentInvoiceRowService.getOrderNumberByInvoiceNumber(invoiceNumber);
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundCustomException("Could not find order with orderNumber: " + orderNumber
                        + " for publishing core sales invoice event"));
        snsPublishService.publishCoreInvoiceReceivedEvent(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder.getInvoiceEvent()));
        log.info("{} delegate invoked", PublishInvoiceDataDelegate.class.getSimpleName());

        var orderFulfillment = salesOrder.getLatestJson().getOrderHeader().getOrderFulfillment();
        if (!equalsIgnoreCase(orderFulfillment, DELTICOM.getName())) {
            metricsHelper.sendCustomEventForInvoices(salesOrder, CORE_INVOICE_PUBLISHED);
        } else {
            metricsHelper.sendCustomEventForInvoices(salesOrder, DROPSHIPMENT_INVOICE_PUBLISHED);
        }
    }
}
