package de.kfzteile24.salesOrderHub.dto.sqs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(value = PropertyNamingStrategy.UpperCamelCaseStrategy.class)
public class SqsMessage {
    String type;
    String messageId;
    String topicArn;
    @JsonProperty("Message")
    String body;
    String timestamp;
    String signatureVersion;
    String signature;
    String signingCertURL;
    String unsubscribeURL;
}
