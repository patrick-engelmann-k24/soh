package de.kfzteile24.salesOrderHub.services.sqs;

import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.readJsonResource;
import static de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper.SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT;
import static de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper.SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID;
import static de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper.SQS_MESSAGE_HEADER_SENDER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PayloadResolverDecoratorIntegrationTest extends AbstractIntegrationTest {

    @SneakyThrows
    protected void setUp() {
        super.setUp();
        doNothing().when(salesOrderCreateService).handleShopOrdersReceived(any(), any());
        PurgeQueueRequest purgeQueueRequest = new PurgeQueueRequest()
                .withQueueUrl(sqsNamesConfig.getEcpShopOrders());

        amazonSQSAsync.purgeQueue(purgeQueueRequest);
    }

    @Test
    void testSqsInvoiceFromCoreListenerDefaultProfile() {
        doReturn(true).when(payloadResolverDecorator).isDefaultProfile();
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(sqsNamesConfig.getInvoicesFromCore())
                .withMessageBody(readJsonResource("invoicesFromCoreSqsMessage.json"));

        amazonSQSAsync.sendMessage(sendMessageRequest);

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                verify(generalSqsReceiveService).queueListenerInvoiceReceivedFromCore(
                        argThat(message -> StringUtils.equals(message, "s3://production-k24-invoices/www-kfzteile24-de/2022/10/07/520292951-724437238.pdf")), any());
            });
    }

    @Test
    void testSqsEcpListenerLocalProfile() {
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(sqsNamesConfig.getEcpShopOrders())
                .withMessageBody(readJsonResource("ecpOrderMessage.json"));

        amazonSQSAsync.sendMessage(sendMessageRequest);

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                verify(payloadResolverDecorator).resolveArgument(any(), argThat(message -> {
                    var headers = message.getHeaders();
                    assertThat(headers)
                            .containsKeys(SQS_MESSAGE_HEADER_SENDER_ID,
                                    SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID,
                                    SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT);
                    assertThat(message.getPayload().toString()).isNotBlank();
                    return true;
                }));

                verify(payloadResolverDecorator).validate(any(), any(), argThat(target -> {
                    assertThat(target).isInstanceOf(Order.class);
                    return true;
                }));

                verify(messageWrapperResolver).resolveArgument(any(), argThat(message -> {
                    var headers = message.getHeaders();
                    assertThat(headers)
                            .containsKeys(SQS_MESSAGE_HEADER_SENDER_ID,
                                    SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID,
                                    SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT);
                    assertThat(message.getPayload().toString()).isNotBlank();
                    return true;
                }));
                verify(salesOrderSqsReceiveService).queueListenerEcpShopOrders(
                        argThat(message -> StringUtils.equals(message.getOrderHeader().getOrderNumber(), "524001240")),
                        argThat(messageWrapper -> {
                            assertThat(messageWrapper.getQueueName()).isEqualTo(sqsNamesConfig.getEcpShopOrders());
                            assertThat(messageWrapper.getReceiveCount()).isEqualTo(1);
                            assertThat(messageWrapper.getSenderId()).isNotBlank();
                            assertThat(messageWrapper.getPayload()).isNotBlank();
                            return true;
                        }));
            });
    }

    @Test
    void testSqsEcpListenerDefaultProfile() {
        doReturn(true).when(payloadResolverDecorator).isDefaultProfile();

        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(sqsNamesConfig.getEcpShopOrders())
                .withMessageBody(readJsonResource("ecpOrderSqsMessage.json"));

        amazonSQSAsync.sendMessage(sendMessageRequest);

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                verify(payloadResolverDecorator).resolveArgument(any(), argThat(message -> {
                    var headers = message.getHeaders();
                    assertThat(headers)
                            .containsKeys(SQS_MESSAGE_HEADER_SENDER_ID,
                                    SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID,
                                    SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT);
                    assertThat(message.getPayload().toString()).isNotBlank();
                    return true;
                }));

                verify(payloadResolverDecorator).validate(any(), any(), argThat(target -> {
                    assertThat(target).isInstanceOf(Order.class);
                    return true;
                }));

                verify(messageWrapperResolver).resolveArgument(any(), argThat(message -> {
                    var headers = message.getHeaders();
                    assertThat(headers)
                            .containsKeys(SQS_MESSAGE_HEADER_SENDER_ID,
                                    SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID,
                                    SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT);
                    assertThat(message.getPayload().toString()).isNotBlank();
                    return true;
                }));
                verify(salesOrderSqsReceiveService).queueListenerEcpShopOrders(
                        argThat(message -> StringUtils.equals(message.getOrderHeader().getOrderNumber(), "524001240")),
                        argThat(messageWrapper -> {
                            assertThat(messageWrapper.getQueueName()).isEqualTo(sqsNamesConfig.getEcpShopOrders());
                            assertThat(messageWrapper.getReceiveCount()).isEqualTo(1);
                            assertThat(messageWrapper.getSenderId()).isNotBlank();
                            assertThat(messageWrapper.getPayload()).isNotBlank();
                            return true;
                        }));
            });
    }

    @Test
    void testSqsEcpListenerExceptionOccurred() {
        doThrow(new SalesOrderNotFoundException("123")).when(salesOrderSqsReceiveService).queueListenerEcpShopOrders(any(), any());
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(sqsNamesConfig.getEcpShopOrders())
                .withMessageBody(readJsonResource("ecpOrderMessage.json"));

        amazonSQSAsync.sendMessage(sendMessageRequest);

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                verify(salesOrderSqsReceiveService, atLeastOnce()).handleExceptions(
                        argThat(exception -> {
                            assertThat(exception).isExactlyInstanceOf(SalesOrderNotFoundException.class);
                            assertThat(exception.getMessage()).isEqualTo("Sales order not found for the given order number 123 ");
                            return true;
                        }),
                        argThat(message -> {
                            var headers = message.getHeaders();
                            assertThat(headers)
                                    .containsKeys(SQS_MESSAGE_HEADER_SENDER_ID,
                                            SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID,
                                            SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT);
                            assertThat(message.getPayload()).isNotBlank();
                            return true;
                        }));
                verify(salesOrderSqsReceiveService, atLeastOnce()).queueListenerEcpShopOrders(any(), any());
            });
    }

    @Test
    void testSqsEcpListenerInvalidOrderJson() {
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(sqsNamesConfig.getEcpShopOrders())
                .withMessageBody(readJsonResource("invalidJsonMessage.json"));

        amazonSQSAsync.sendMessage(sendMessageRequest);

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                verify(salesOrderSqsReceiveService, atLeastOnce()).handleMethodArgumentNotValidExceptions(
                        argThat(exception -> {
                            assertThat(exception.getBindingResult()).isNotNull();
                            assertThat(exception.getBindingResult().getFieldErrors()).isNotEmpty();
                            return true;
                        }));
                verify(salesOrderSqsReceiveService, never()).queueListenerEcpShopOrders(any(), any());
            });
    }

    @Test
    void testSqsInvoiceFromCoreListenerLocalProfile() {
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(sqsNamesConfig.getInvoicesFromCore())
                .withMessageBody(readJsonResource("invoicesFromCoreMessage.json"));

        amazonSQSAsync.sendMessage(sendMessageRequest);

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                verify(generalSqsReceiveService).queueListenerInvoiceReceivedFromCore(
                        argThat(message -> StringUtils.equals(message, "s3://production-k24-invoices/www-kfzteile24-de/2022/10/07/520292951-724437238.pdf")), any());
            });
    }

    @AfterEach
    void cleanUp() {
        PurgeQueueRequest purgeQueueRequest = new PurgeQueueRequest()
                .withQueueUrl(sqsNamesConfig.getEcpShopOrders());

        amazonSQSAsync.purgeQueue(purgeQueueRequest);
    }
}