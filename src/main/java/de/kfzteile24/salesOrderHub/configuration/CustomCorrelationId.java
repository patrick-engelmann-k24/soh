package de.kfzteile24.salesOrderHub.configuration;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.zalando.logbook.CorrelationId;
import org.zalando.logbook.HttpRequest;

import java.util.UUID;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.TRACE_ID_NAME;

@Slf4j
public class CustomCorrelationId implements CorrelationId {

    @Override
    public String generate(HttpRequest request) {
        String traceId = MDC.get(TRACE_ID_NAME);
        request.getHeaders().update(TRACE_ID_NAME, traceId);
        updateLoggerMDC();
        return traceId;
    }

    public static void updateLoggerMDC() {
        String correlationId = generateCorrelationId();
        MDC.put(TRACE_ID_NAME, correlationId);
    }

    public static String generateCorrelationId() {
//        final Random random = ThreadLocalRandom.current();
//        // set most significant bit to produce fixed length string
//        return Long.toHexString(random.nextLong() | Long.MIN_VALUE);
        return UUID.randomUUID().toString();
    }
}
