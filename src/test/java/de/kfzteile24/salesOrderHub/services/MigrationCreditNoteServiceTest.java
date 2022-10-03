package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.services.financialdocuments.CoreSalesCreditNoteCreatedService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationCreditNoteServiceTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SnsPublishService snsPublishService;

    @Spy
    private final SQSNamesConfig sqsNamesConfig = new SQSNamesConfig();

    @Mock
    private CreditNoteEventMapper creditNoteEventMapper;

    @Mock
    private SalesOrderReturnService salesOrderReturnService;

    @Mock
    private CoreSalesCreditNoteCreatedService coreSalesCreditNoteCreatedService;

    @Mock
    private FeatureFlagConfig featureFlagConfig;

    @Mock
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;

    @InjectMocks
    private MigrationCreditNoteService migrationCreditNoteService;

    private final MessageWrapper messageWrapper = MessageWrapper.builder().build();

    @Test
    void testHandleMigrationCoreSalesCreditNoteCreatedDuplication() {

        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        var orderNumber = message.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        var creditNoteNumber = message.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber();

        SalesOrder salesOrder = createSalesOrder(orderNumber);
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, creditNoteNumber);
        when(salesOrderReturnService.getByOrderNumber(salesOrderReturn.getOrderNumber())).thenReturn(Optional.of(salesOrderReturn));
        when(salesOrderService.createOrderNumberInSOH(orderNumber, creditNoteNumber)).thenReturn(createOrderNumberInSOH(orderNumber, creditNoteNumber));

        migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(message, messageWrapper);

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(salesOrderReturn);
    }

    @Test
    void testHandleMigrationCoreSalesCreditNoteCreatedNewCreditNote() {

        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        var orderNumber = message.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        var creditNoteNumber = message.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber();

        when(salesOrderReturnService.getByOrderNumber(any())).thenReturn(Optional.empty());
        when(salesOrderService.createOrderNumberInSOH(orderNumber, creditNoteNumber)).thenReturn(createOrderNumberInSOH(orderNumber, creditNoteNumber));

        migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(message, messageWrapper);

        verify(financialDocumentsSqsReceiveService).queueListenerCoreSalesCreditNoteCreated(message, messageWrapper);
    }

    private SalesOrder createSalesOrder(String orderNumber) {
        var order = getObjectByResource("ecpOrderMessage.json", Order.class);
        order.getOrderHeader().setOrderNumber(orderNumber);
        order.getOrderHeader().setOrderGroupId(orderNumber);
        return createSalesOrderFromOrder(order);
    }
}