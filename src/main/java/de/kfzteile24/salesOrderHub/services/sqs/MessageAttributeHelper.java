package de.kfzteile24.salesOrderHub.services.sqs;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageAttributeHelper {

    private final AmazonSQSAsync amazonSQSAsync;

    public void moveToDlq(MessageWrapper messageWrapper, Throwable e) {
        Map<String, MessageAttributeValue> messageAttributes = createStringMessageAttributeValueMap(e);
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(messageWrapper.getQueueName() + "-dlq")
                .withMessageBody(messageWrapper.getPayload())
                .withMessageAttributes(messageAttributes)
                .withDelaySeconds(1);
        amazonSQSAsync.sendMessage(sendMessageRequest);
        log.info("Message for {} was manually sent to DLQ", messageWrapper.getQueueName());
    }


    private Map<String, MessageAttributeValue> createStringMessageAttributeValueMap(Throwable e) {

        MessageAttributeValue exceptionMessageAttribute = new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(e.toString());
        MessageAttributeValue stacktraceMessageAttribute = new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(Arrays.toString(e.getStackTrace()));
        return Map.of(
                "exception", exceptionMessageAttribute,
                "stacktrace", stacktraceMessageAttribute);
    }
}
