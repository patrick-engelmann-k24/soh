package de.kfzteile24.salesOrderHub.configuration;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.zalando.logbook.CorrelationId;
import org.zalando.logbook.HttpRequest;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.TRACE_ID_NAME;

@Slf4j
public class CustomCorrelationId implements CorrelationId {

    @Override
    public String generate(HttpRequest request) {
        updateLoggerMDC();
        return MDC.get(TRACE_ID_NAME);
    }

    public static void updateLoggerMDC() {
        String correlationId = generateCorrelationId();
        MDC.put(TRACE_ID_NAME, correlationId);
    }

    public static String generateCorrelationId() {
        final Random random = ThreadLocalRandom.current();
        return Long.toHexString(random.nextLong() | Long.MIN_VALUE);
    }
}
