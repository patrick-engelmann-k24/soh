package de.kfzteile24.salesOrderHub.helper;


import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SleuthHelper {

    public static final String TRACE_ID_NAME = "x-business-key";

    private final Tracer tracer;

    public void updateTraceId(String traceId) {
        MDC.put(TRACE_ID_NAME, traceId);
        Optional.ofNullable(tracer.getBaggage(TRACE_ID_NAME))
                .ifPresent(b -> b.set(traceId));
    }
}
