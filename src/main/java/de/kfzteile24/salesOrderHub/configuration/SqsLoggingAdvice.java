package de.kfzteile24.salesOrderHub.configuration;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
@Slf4j
class SqsLoggingAdvice {

    @Around("execution(public void de.kfzteile24.salesOrderHub.services.SqsReceiveService.*(String, String, Integer))")
    Object incomingMessageLogging(ProceedingJoinPoint joinPoint) throws Throwable {
        logReceivedMessage(joinPoint.getArgs());
        return joinPoint.proceed();
    }

    private static void logReceivedMessage(Object... args) {
        log.info("message received: {}\r\nmessage receive count: {}\r\nmessage content: {}",
            args[1], args[2], args[0]);
    }
}
