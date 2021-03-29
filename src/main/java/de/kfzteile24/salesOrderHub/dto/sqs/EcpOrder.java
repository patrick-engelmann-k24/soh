package de.kfzteile24.salesOrderHub.dto.sqs;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(value = PropertyNamingStrategy.UpperCamelCaseStrategy.class)
public class EcpOrder {
    String type;
    String messageId;
    String topicArn;
    String message;
    String timestamp;
    String signatureVersion;
    String signature;
    String signingCertURL;
    String unsubscribeURL;
}
