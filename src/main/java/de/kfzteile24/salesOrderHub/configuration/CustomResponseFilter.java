package de.kfzteile24.salesOrderHub.configuration;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.ResponseFilter;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.TRACE_ID_NAME;

@Slf4j
public class CustomResponseFilter implements ResponseFilter {

    @Override
    public HttpResponse filter(HttpResponse response) {
        String traceId = MDC.get(TRACE_ID_NAME);
        log.info("TRACE_ID : " + traceId);
        HttpHeaders headers = response.getHeaders().update("RequestID", traceId);
        return new CustomHttpResponse(headers, response);
    }
}
