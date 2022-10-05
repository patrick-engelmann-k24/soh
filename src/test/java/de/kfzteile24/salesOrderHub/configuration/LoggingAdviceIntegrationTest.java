package de.kfzteile24.salesOrderHub.configuration;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.general.GeneralSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.salesorder.SalesOrderSqsReceiveService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@Slf4j
class LoggingAdviceIntegrationTest extends AbstractIntegrationTest {

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
        final String rawMessage = getRawMessageOfOrderWithoutCustomerNumber();

        salesOrderSqsReceiveService.queueListenerEcpShopOrders(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getEcpShopOrders() + "-dlq");

        verifyErrorMessageIsLogged(() -> salesOrderSqsReceiveService.queueListenerEcpShopOrders(rawMessage, "senderId", 3),
                rawMessage, sqsNamesConfig.getEcpShopOrders());
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Braincraft Shop Orders")
    void testMessageAttributeForBcShop(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = getRawMessageOfOrderWithoutCustomerNumber();

        salesOrderSqsReceiveService.queueListenerBcShopOrders(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getBcShopOrders() + "-dlq");

        verifyErrorMessageIsLogged(() -> salesOrderSqsReceiveService.queueListenerBcShopOrders(rawMessage, "senderId", 3),
                rawMessage, sqsNamesConfig.getBcShopOrders());
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Core Shop Orders")
    void testMessageAttributeForCoreShop(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = getRawMessageOfOrderWithoutCustomerNumber();

        salesOrderSqsReceiveService.queueListenerCoreShopOrders(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getCoreShopOrders() + "-dlq");

        verifyErrorMessageIsLogged(() -> salesOrderSqsReceiveService.queueListenerCoreShopOrders(rawMessage, "senderId", 3),
                rawMessage, sqsNamesConfig.getCoreShopOrders());
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Invoice Created Event")
    void testMessageAttributeForInvoiceCreatedEvent(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = readResource("examples/coreSalesInvoiceCreatedOneItem.json");

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getCoreSalesInvoiceCreated() + "-dlq");

        verifyErrorMessageIsLogged(() -> financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(rawMessage, "senderId", 3),
                rawMessage, sqsNamesConfig.getCoreSalesInvoiceCreated());
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Parcel Shipped Event")
    void testMessageAttributeForParcelShippedEvent(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = readResource("examples/parcelShipped.json");

        generalSqsReceiveService.queueListenerParcelShipped(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ(sqsNamesConfig.getParcelShipped() + "-dlq");

        verifyErrorMessageIsLogged(() -> generalSqsReceiveService.queueListenerParcelShipped(rawMessage, "senderId", 3),
                rawMessage, sqsNamesConfig.getParcelShipped());
    }

    @NotNull
    private String getRawMessageOfOrderWithoutCustomerNumber() {
        String rawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        return rawMessage.replace("DEB-000000000", "");
    }

    private void verifyIfMessageIsSendingToDLQ(String dlqName) {
        verify(amazonSQSAsync).sendMessage(argThat(msgReq -> {
            assertThat(msgReq.getQueueUrl()).contains(dlqName);
            return true;
        }));
    }

    private void verifyErrorMessageIsLogged(Runnable runnable, String rawMessage, String queueName) {
        try {
            runnable.run();
            fail("should throw exception");
        } catch (Exception e) {
            //ignore
        }
        verify(messageErrorHandler).logErrorMessage(eq(rawMessage), eq(queueName), eq(3), any());
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return java.nio.file.Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                        .toURI()));
    }
}