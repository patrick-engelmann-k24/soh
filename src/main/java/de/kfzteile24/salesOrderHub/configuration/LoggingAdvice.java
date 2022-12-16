package de.kfzteile24.salesOrderHub.configuration;

import de.kfzteile24.salesOrderHub.helper.CustomValidator;
import de.kfzteile24.salesOrderHub.services.sqs.MessageAttributeHelper;
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

import static de.kfzteile24.salesOrderHub.services.sqs.AbstractSqsReceiveService.logErrorMessage;

@Aspect
@Configuration
@Slf4j
@RequiredArgsConstructor
class LoggingAdvice {

    private final SleuthHelper sleuthHelper;
    private final MessageAttributeHelper messageAttributeHelper;
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

        if (message != null) {
            try {
                customValidator.validate(message);
                return joinPoint.proceed();
            } catch (ConstraintViolationException e) {
                messageAttributeHelper.moveToDlq(messageWrapper, e);
            } catch (Throwable e) {
                if (messageWrapper.getReceiveCount() < 4) {
                    logErrorMessage(messageWrapper, e);
                } else {
                    messageAttributeHelper.moveToDlq(messageWrapper, e);
                }
            }
        }
        return null;
    }
}
