package de.kfzteile24.salesOrderHub.configuration;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import de.kfzteile24.salesOrderHub.helper.CustomValidator;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
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

import javax.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.services.sqs.AbstractSqsReceiveService.logErrorMessage;

@Aspect
@Configuration
@Slf4j
@RequiredArgsConstructor
class LoggingAdvice {

    private final SleuthHelper sleuthHelper;
    private final AmazonSQSAsync amazonSQSAsync;
    private final CustomValidator customValidator;

    @Around("execution(public void *.notify(org.camunda.bpm.engine.delegate.DelegateExecution))")
    Object updateTraceContext(ProceedingJoinPoint joinPoint) throws Throwable {
        DelegateExecution delegateExecution = (DelegateExecution) joinPoint.getArgs()[0];
        sleuthHelper.updateTraceId(delegateExecution.getBusinessKey());
        return joinPoint.proceed();
    }

    @SneakyThrows
    @Around("@annotation(enrichMessageForDlq)")
    Object aroundServiceAdvice(ProceedingJoinPoint joinPoint, EnrichMessageForDlq enrichMessageForDlq) {

        var message = joinPoint.getArgs()[0];
        var messageWrapper = (MessageWrapper) joinPoint.getArgs()[1];
        log.info("This is the logging advice with received count: {}", messageWrapper.getReceiveCount());

        try {
            customValidator.validate(message);
            return joinPoint.proceed();
        } catch (ConstraintViolationException e) {
            moveToDlq(messageWrapper, e);
        } catch (Throwable e) {
            if (messageWrapper.getReceiveCount() < 4) {
                logErrorMessage(messageWrapper, e);
            } else {
                moveToDlq(messageWrapper, e);
            }
        }
        return null;
    }

    private void moveToDlq(MessageWrapper messageWrapper, Throwable e) {
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
