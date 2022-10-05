package de.kfzteile24.salesOrderHub.configuration;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;

import java.io.IOException;

public class CustomHttpResponse implements HttpResponse {

    private final HttpHeaders headers;
    private final HttpResponse response;

    public CustomHttpResponse(HttpHeaders headers, HttpResponse response) {
        this.response = response;
        this.headers = headers;
    }

    @Override
    public int getStatus() {
        return response.getStatus();
    }

    @Override
    public HttpResponse withBody() throws IOException {
        return response.withBody();
    }

    @Override
    public HttpResponse withoutBody() {
        return response.withoutBody();
    }

    @Override
    public Origin getOrigin() {
        return response.getOrigin();
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public byte[] getBody() throws IOException {
        return response.getBody();
    }
}
