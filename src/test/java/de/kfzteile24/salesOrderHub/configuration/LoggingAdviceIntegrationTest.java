package de.kfzteile24.salesOrderHub.configuration;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import de.kfzteile24.salesOrderHub.services.sqs.SqsReceiveService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Slf4j
class LoggingAdviceIntegrationTest {

    @Autowired
    private SqsReceiveService sqsReceiveService;

    @SpyBean
    private AmazonSQSAsync amazonSQSAsync;

    @BeforeEach
    void setUp() {
        doReturn(null).when(amazonSQSAsync).sendMessage(any());
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Ecp Shop Orders")
    void testMessageAttributeForEcpShop(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = getRawMessageOfOrderWithoutCustomerNumber();

        sqsReceiveService.queueListenerEcpShopOrders(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ("soh-ecp-shop-orders-queue-dlq");
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Braincraft Shop Orders")
    void testMessageAttributeForBcShop(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = getRawMessageOfOrderWithoutCustomerNumber();

        sqsReceiveService.queueListenerBcShopOrders(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ("soh-bc-shop-orders-queue-dlq");
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Core Shop Orders")
    void testMessageAttributeForCoreShop(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = getRawMessageOfOrderWithoutCustomerNumber();

        sqsReceiveService.queueListenerCoreShopOrders(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ("soh-core-shop-orders-queue-dlq");
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Invoice Created Event")
    void testMessageAttributeForInvoiceCreatedEvent(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = readResource("examples/coreSalesInvoiceCreatedOneItem.json");

        sqsReceiveService.queueListenerCoreSalesInvoiceCreated(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ("soh-core-sales-invoice-created-queue-dlq");
    }

    @Test
    @DisplayName("Test Logging Advice Message Attribute Property for Parcel Shipped Event")
    void testMessageAttributeForParcelShippedEvent(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = readResource("examples/parcelShipped.json");

        sqsReceiveService.queueListenerParcelShipped(rawMessage, "senderId", 4);
        verifyIfMessageIsSendingToDLQ("soh-parcel-shipped-queue-dlq");
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

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return java.nio.file.Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                        .toURI()));
    }
}