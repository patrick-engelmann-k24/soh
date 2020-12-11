package de.kfzteile24.salesOrderHub.dto.aws;

import lombok.Data;

import java.util.Date;

@Data
public class MessageHeader {
    String type;
    String messageId;
    String topicArn;
    String message;
    Date timestamp;
    String signatureVersion;
    String signature;
    String signingCertURL;
    String unsubscribeURL;
}
