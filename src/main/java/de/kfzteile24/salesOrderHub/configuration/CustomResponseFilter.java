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
        HttpHeaders headers = response.getHeaders().update("RequestID", MDC.get(TRACE_ID_NAME));
        log.info("New headers " + headers);
        CustomHttpResponse customHttpResponse = new CustomHttpResponse(headers, response);
        log.info("Custom Http Response " + customHttpResponse);
        return customHttpResponse;
    }
}
