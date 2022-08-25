package de.kfzteile24.salesOrderHub.helper;


import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.sleuth.BaggageInScope;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SleuthHelper {

    public static final String TRACE_ID_NAME = "x-business-key";

    private final Tracer tracer;

    public void updateTraceId(String traceId) {
        Optional.ofNullable(tracer.getBaggage(TRACE_ID_NAME))
                .ifPresent(b -> b.set(traceId));
    }

    public String getTracIdValue() {
        return Optional.ofNullable(tracer.getBaggage(TRACE_ID_NAME))
                .map(BaggageInScope::get)
                .orElse(StringUtils.EMPTY);
    }
}
