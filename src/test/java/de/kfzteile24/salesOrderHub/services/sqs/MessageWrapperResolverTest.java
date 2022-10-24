package de.kfzteile24.salesOrderHub.services.sqs;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.support.GenericMessage;

import java.lang.reflect.Method;
import java.util.HashMap;

import static de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper.SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT;
import static de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper.SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID;
import static de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper.SQS_MESSAGE_HEADER_SENDER_ID;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MessageWrapperResolverTest {

    public static final String ANY_PAYLOAD = RandomStringUtils.randomAlphabetic(245);
    public static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(5);
    public static final String ANY_QUEUE_NAME = RandomStringUtils.randomAlphabetic(5);
    public static final String ANY_RECEIVE_COUNT = "1";

    @Spy
    private MessageWrapperResolver messageWrapperResolver;


    @Test
    void resolveArgument() throws Exception {
        var headers = new HashMap<String, Object>();
        headers.put(SQS_MESSAGE_HEADER_SENDER_ID, ANY_SENDER_ID);
        headers.put(SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID, ANY_QUEUE_NAME);
        headers.put(SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT, ANY_RECEIVE_COUNT);
        var message = new GenericMessage<>(ANY_PAYLOAD, headers);

        Method method = this.getClass().getDeclaredMethods()[0];
        MethodParameter methodParameter = new MethodParameter(method, -1);

        var argument = messageWrapperResolver.resolveArgument(methodParameter, message);

        assertThat(argument).isExactlyInstanceOf(MessageWrapper.class);
        var messageWrapper = (MessageWrapper) argument;

        assertThat(messageWrapper).isNotNull();
        assertThat(messageWrapper.getPayload()).isEqualTo(ANY_PAYLOAD);
        assertThat(messageWrapper.getQueueName()).isEqualTo(ANY_QUEUE_NAME);
        assertThat(messageWrapper.getReceiveCount()).isEqualTo(1);
        assertThat(messageWrapper.getSenderId()).isEqualTo(ANY_SENDER_ID);
    }
}