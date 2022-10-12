package de.kfzteile24.salesOrderHub.services.sqs;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.messaging.Message;

import java.util.Objects;

@Getter
@Setter
@Builder
public class MessageWrapper {

    public static final String SQS_MESSAGE_HEADER_SENDER_ID = "SenderId";
    public static final String SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID = "LogicalResourceId";
    public static final String SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT = "ApproximateReceiveCount";
    
    private String senderId;
    private Integer receiveCount;
    private String queueName;
    private String payload;

    public String getSanitizedPayload() {
        return RegExUtils.removeAll(payload, "[\\t\\n\\r]+");
    }

    public static MessageWrapper fromMessage(@NonNull Message<?> message) {
        var headers = message.getHeaders();
        return MessageWrapper.builder()
                .senderId(headers.get(SQS_MESSAGE_HEADER_SENDER_ID, String.class))
                .queueName(headers.get(SQS_MESSAGE_HEADER_LOGICAL_RESOURCE_ID, String.class))
                .receiveCount(Integer.valueOf(Objects.requireNonNull(headers.get(SQS_MESSAGE_HEADER_APPROXIMATE_RECEIVE_COUNT, String.class))))
                .payload(message.getPayload().toString())
                .build();
    }
}
