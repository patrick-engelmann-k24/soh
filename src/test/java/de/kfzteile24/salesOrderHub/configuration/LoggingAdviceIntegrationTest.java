package de.kfzteile24.salesOrderHub.configuration;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.general.GeneralSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.salesorder.SalesOrderSqsReceiveService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@Slf4j
class LoggingAdviceIntegrationTest extends AbstractIntegrationTest {

    public static final String SENDER_ID = RandomStringUtils.randomAlphabetic(5);

    @Autowired
    private SalesOrderSqsReceiveService salesOrderSqsReceiveService;
    @Autowired
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;
    @Autowired
    private GeneralSqsReceiveService generalSqsReceiveService;
    @Autowired
    private SQSNamesConfig sqsNamesConfig;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        doReturn(null).when(amazonSQSAsync).sendMessage(any());
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Ecp Shop Orders")
    void testMessageAttributeForEcpShop(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        var message = getRawMessageOfOrderWithoutCustomerNumber();
        var messageWrapper = MessageWrapper.builder()
                .senderId(SENDER_ID)
                .queueName(sqsNamesConfig.getEcpShopOrders())
                .receiveCount(4)
                .build();

        salesOrderSqsReceiveService.queueListenerEcpShopOrders(message, messageWrapper);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getEcpShopOrders() + "-dlq");

        messageWrapper.setReceiveCount(3);

        verifyErrorMessageIsLogged(() -> salesOrderSqsReceiveService.queueListenerEcpShopOrders(message, messageWrapper));
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Braincraft Shop Orders")
    void testMessageAttributeForBcShop(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        var message = getRawMessageOfOrderWithoutCustomerNumber();
        var messageWrapper = MessageWrapper.builder()
                .senderId(SENDER_ID)
                .queueName(sqsNamesConfig.getBcShopOrders())
                .receiveCount(4)
                .build();

        salesOrderSqsReceiveService.queueListenerBcShopOrders(message, messageWrapper);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getBcShopOrders() + "-dlq");

        messageWrapper.setReceiveCount(3);

        verifyErrorMessageIsLogged(() -> salesOrderSqsReceiveService.queueListenerBcShopOrders(message, messageWrapper));
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Core Shop Orders")
    void testMessageAttributeForCoreShop(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        var message = getRawMessageOfOrderWithoutCustomerNumber();
        var messageWrapper = MessageWrapper.builder()
                .senderId(SENDER_ID)
                .queueName(sqsNamesConfig.getCoreShopOrders())
                .receiveCount(4)
                .build();

        salesOrderSqsReceiveService.queueListenerCoreShopOrders(message, messageWrapper);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getCoreShopOrders() + "-dlq");

        messageWrapper.setReceiveCount(3);

        verifyErrorMessageIsLogged(() -> salesOrderSqsReceiveService.queueListenerCoreShopOrders(message, messageWrapper));
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Invoice Created Event")
    void testMessageAttributeForInvoiceCreatedEvent(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder()
                .senderId(SENDER_ID)
                .queueName(sqsNamesConfig.getCoreSalesInvoiceCreated())
                .receiveCount(4)
                .build();

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getCoreSalesInvoiceCreated() + "-dlq");

        messageWrapper.setReceiveCount(3);

        verifyErrorMessageIsLogged(() -> financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper));
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Parcel Shipped Event")
    void testMessageAttributeForParcelShippedEvent(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        var message = getObjectByResource("parcelShipped.json", ParcelShippedMessage.class);
        var messageWrapper = MessageWrapper.builder()
                .senderId(SENDER_ID)
                .queueName(sqsNamesConfig.getParcelShipped())
                .receiveCount(4)
                .build();

        generalSqsReceiveService.queueListenerParcelShipped(message, messageWrapper);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getParcelShipped() + "-dlq");

        messageWrapper.setReceiveCount(3);

        verifyErrorMessageIsLogged(() -> generalSqsReceiveService.queueListenerParcelShipped(message, messageWrapper));
    }

    @NotNull
    private Order getRawMessageOfOrderWithoutCustomerNumber() {
        var message = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        message.getOrderHeader().getCustomer().setCustomerNumber(null);
        return message;
    }

    private void verifyIfMessageIsSendingToDLQ(String dlqName) {
        verify(amazonSQSAsync).sendMessage(argThat(msgReq -> {
            assertThat(msgReq.getQueueUrl()).contains(dlqName);
            return true;
        }));
    }

    private void verifyErrorMessageIsLogged(Runnable runnable) {
        try {
            runnable.run();
            fail("should throw exception");
        } catch (Exception e) {
            //ignore
        }
    }
}