package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AwsSnsConfig {

    @Value("${soh.sns.topic.orderCreatedV2}")
    private String snsOrderCreatedTopicV2;

    @Value("${soh.sns.topic.orderCompleted}")
    private String snsOrderCompletedTopic;

    @Value("${soh.sns.topic.invoiceAddressChanged}")
    private String snsInvoiceAddressChangedTopic;

    @Value("${soh.sns.topic.deliveryAddressChanged}")
    private String snsDeliveryAddressChanged;

    @Value("${soh.sns.topic.salesOrderRowCancelled}")
    private String snsSalesOrderRowCancelled;

    @Value("${soh.sns.topic.salesOrderCancelled}")
    private String snsSalesOrderCancelled;

    @Value("${soh.sns.topic.orderInvoiceCreatedV1}")
    private String snsOrderInvoiceCreatedV1;

    @Value("${soh.sns.topic.shipmentConfirmedV1}")
    private String snsShipmentConfirmedV1;

    @Value("${soh.sns.topic.returnOrderCreatedV1}")
    private String snsReturnOrderCreatedV1;

    @Value("${soh.sns.topic.coreInvoiceReceivedV1}")
    private String snsCoreInvoiceReceivedV1;

    @Value("${soh.sns.topic.creditNoteReceivedV1}")
    private String snsCreditNoteReceivedV1;

    @Value("${soh.sns.topic.creditNoteCreatedV1}")
    private String snsCreditNoteCreatedV1;

    @Value("${soh.sns.topic.creditNoteDocumentGeneratedV1}")
    private String snsCreditNoteDocumentGeneratedV1;

    @Value("${soh.sns.topic.migrationOrderCreatedV2}")
    private String snsMigrationOrderCreatedV2;

    @Value("${soh.sns.topic.migrationSalesOrderRowCancelledV1}")
    private String snsMigrationSalesOrderRowCancelledV1;
  
    @Value("${soh.sns.topic.migrationSalesOrderCancelledV1}")
    private String snsMigrationSalesOrderCancelledV1;

    @Value("${soh.sns.topic.migrationReturnOrderCreatedV1}")
    private String snsMigrationReturnOrderCreatedV1;

    @Value("${soh.sns.topic.dropshipmentOrderCreatedV1}")
    private String snsDropshipmentOrderCreatedV1;

    @Value("${soh.sns.topic.dropshipmentOrderReturnNotifiedV1}")
    private String snsDropshipmentOrderReturnNotifiedV1;

    @Value("${soh.sns.topic.payoutReceiptConfirmationReceivedV1}")
    private String snsPayoutReceiptConfirmationReceivedV1;

    @Value("${soh.sns.topic.invoicePdfGenerationTriggeredV1}")
    private String snsInvoicePdfGenerationTriggeredV1;

}
