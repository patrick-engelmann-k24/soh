package de.kfzteile24.salesOrderHub.configuration;

import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
@Slf4j
@RequiredArgsConstructor
class LoggingAdvice {

    private final SleuthHelper sleuthHelper;

    @Around("execution(public void de.kfzteile24.salesOrderHub.services.sqs.SqsReceiveService.*(String, String, Integer))")
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
}
