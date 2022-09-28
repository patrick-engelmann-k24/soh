package de.kfzteile24.salesOrderHub.configuration;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;

@Aspect
@Configuration
@Slf4j
@RequiredArgsConstructor
class LoggingAdvice {

    private final SleuthHelper sleuthHelper;
    private final AmazonSQSAsync amazonSQSAsync;
    private final MessageErrorHandler messageErrorHandler;

    @Around("execution(public void de.kfzteile24.salesOrderHub.services.salesorder.SalesOrderSqsReceiveService.*(String, String, Integer)) || " +
            "execution(public void de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService.*(String, String, Integer)) || " +
            "execution(public void de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentSqsReceiveService.*(String, String, Integer)) || " +
            "execution(public void de.kfzteile24.salesOrderHub.services.migration.MigrationSqsReceiveService.*(String, String, Integer)) || " +
            "execution(public void de.kfzteile24.salesOrderHub.services.general.GeneralSqsReceiveService.*(String, String, Integer))")
    Object incomingMessageLogging(ProceedingJoinPoint joinPoint) throws Throwable {
        logReceivedMessage(joinPoint.getArgs());
        return joinPoint.proceed();
    }

    @Around("execution(public void *.notify(org.camunda.bpm.engine.delegate.DelegateExecution))")
    Object updateTraceContext(ProceedingJoinPoint joinPoint) throws Throwable {
        DelegateExecution delegateExecution = (DelegateExecution) joinPoint.getArgs()[0];
        sleuthHelper.updateTraceId(delegateExecution.getBusinessKey());
        return joinPoint.proceed();
    }

    private static void logReceivedMessage(Object... args) {
        log.info("message received: {}\r\nmessage receive count: {}\r\nmessage content: {}",
                args[1], args[2], args[0]);
    }

    @SneakyThrows
    @Around("@annotation(enrichMessageForDlq)")
    Object moveMessageToDlq(ProceedingJoinPoint joinPoint, EnrichMessageForDlq enrichMessageForDlq) {

        String rawMessage = (String) joinPoint.getArgs()[0];
        Integer receiveCount = (Integer) joinPoint.getArgs()[1];
        String queueName = (String) joinPoint.getArgs()[2];
        log.info("This is the logging advice with received count: {}", receiveCount);

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            if (receiveCount < 4) {
                messageErrorHandler.logErrorMessage(rawMessage, queueName, receiveCount, e);
            } else {
                moveToDlq(rawMessage, queueName, e);
            }
        }
        return null;
    }

    private void moveToDlq(String rawMessage, String queueName, Throwable e) {
        Map<String, MessageAttributeValue> messageAttributes = createStringMessageAttributeValueMap(e);
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueName + "-dlq")
                .withMessageBody(rawMessage)
                .withMessageAttributes(messageAttributes)
                .withDelaySeconds(1);
        amazonSQSAsync.sendMessage(sendMessageRequest);
        log.info("Message for invoice received was manually sent to DLQ");
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
