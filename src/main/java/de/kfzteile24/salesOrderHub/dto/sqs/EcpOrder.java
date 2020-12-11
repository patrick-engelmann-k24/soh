package de.kfzteile24.salesOrderHub.dto.sqs;

import lombok.Data;

@Data
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
