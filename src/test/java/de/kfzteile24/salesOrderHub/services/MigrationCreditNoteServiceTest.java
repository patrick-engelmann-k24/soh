package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.services.financialdocuments.CoreSalesCreditNoteCreatedService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getCreditNoteMsg;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationCreditNoteServiceTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SnsPublishService snsPublishService;

    @Spy
    private SQSNamesConfig sqsNamesConfig = new SQSNamesConfig();

    @Mock
    private CreditNoteEventMapper creditNoteEventMapper;

    @Mock
    private SalesOrderReturnService salesOrderReturnService;

    @Mock
    private CoreSalesCreditNoteCreatedService coreSalesCreditNoteCreatedService;

    @Mock
    private MessageWrapperUtil messageWrapperUtil;

    @Mock
    private FeatureFlagConfig featureFlagConfig;

    @Mock
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;

    @InjectMocks
    private MigrationCreditNoteService migrationCreditNoteService;

    @Test
    void testHandleMigrationCoreSalesCreditNoteCreatedDuplication() {

        String rawEventMessage = readResource("examples/coreSalesCreditNoteCreated.json");
        mockMessageWrapper(rawEventMessage,  SalesCreditNoteCreatedMessage.class);
        var creditNoteMsg = getCreditNoteMsg(rawEventMessage);
        var orderNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        var creditNoteNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber();

        SalesOrder salesOrder = createSalesOrder(orderNumber);
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, creditNoteNumber);
        when(salesOrderReturnService.getByOrderNumber(eq(salesOrderReturn.getOrderNumber()))).thenReturn(salesOrderReturn);
        when(salesOrderService.createOrderNumberInSOH(eq(orderNumber), eq(creditNoteNumber))).thenReturn(createOrderNumberInSOH(orderNumber, creditNoteNumber));

        migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(rawEventMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(salesOrderReturn);
    }

    @Test
    void testHandleMigrationCoreSalesCreditNoteCreatedNewCreditNote() {

        String rawEventMessage = readResource("examples/coreSalesCreditNoteCreated.json");
        mockMessageWrapper(rawEventMessage,  SalesCreditNoteCreatedMessage.class);
        var creditNoteMsg = getCreditNoteMsg(rawEventMessage);
        var orderNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        var creditNoteNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber();

        when(salesOrderReturnService.getByOrderNumber(any())).thenReturn(null);
        when(salesOrderService.createOrderNumberInSOH(eq(orderNumber), eq(creditNoteNumber))).thenReturn(createOrderNumberInSOH(orderNumber, creditNoteNumber));

        migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(rawEventMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(financialDocumentsSqsReceiveService).queueListenerCoreSalesCreditNoteCreated(rawEventMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);
    }

    private SalesOrder createSalesOrder(String orderNumber) {
        String rawOrderMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(rawOrderMessage);
        order.getOrderHeader().setOrderNumber(orderNumber);
        order.getOrderHeader().setOrderGroupId(orderNumber);
        return createSalesOrderFromOrder(order);
    }

    @SneakyThrows
    private <T> void mockMessageWrapper(String rawMessage, Class<T> clazz) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        T message = objectMapper.readValue(body, clazz);
        var messageWrapper = MessageWrapper.<T>builder()
                .message(message)
                .rawMessage(rawMessage)
                .build();
        when(messageWrapperUtil.create(eq(rawMessage), eq(clazz)))
                .thenReturn(messageWrapper);
    }
}