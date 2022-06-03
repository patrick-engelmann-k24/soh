package de.kfzteile24.salesOrderHub.services.sqs;

import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageWrapper<T> {

    private final String rawMessage;
    private final SqsMessage sqsMessage;
    private final T message;
}
