package de.kfzteile24.salesOrderHub.clients;

import de.kfzteile24.salesOrderHub.configuration.PricingServiceConfig;
import de.kfzteile24.salesOrderHub.dto.pricing.SetUnitPriceAPIResponse;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.util.Optional;
import java.util.function.Function;

import static org.springframework.http.HttpMethod.GET;

@Slf4j
@RequiredArgsConstructor
@Component
public class PricingServiceClient {
    private static final String GET_PRICE_ENDPOINT =
            "/?product_number={productNumber}&sales_channel_code={salesChannelCode}";

    private final RestTemplate restTemplate;
    private final PricingServiceConfig pricingServiceConfig;

    @Timed("fetch.price")
    public Optional<SetUnitPriceAPIResponse> getSetPriceInfo(String sku, String salesChannelCode) {
        return executeRequest((authHeaders) -> {
            var endpoint = new UriTemplate(pricingServiceConfig.getUrl() + GET_PRICE_ENDPOINT)
                    .expand(sku, salesChannelCode);
            log.info("Calling pricing service endpoint: {}", endpoint);
            final var httpEntity = new HttpEntity<>(null, authHeaders);
            try {
                final SetUnitPriceAPIResponse response =
                        restTemplate.exchange(endpoint, GET, httpEntity, SetUnitPriceAPIResponse.class).getBody();

                return Optional.ofNullable(
                        response == null ||
                                response.getSetParentProductNumber() == null ||
                                response.getSetUnitPrices() == null ||
                                response.getSetUnitPrices().get(0) == null ||
                                response.getSetUnitPrices().get(0).getSku() == null ?
                                null : response
                );
            } catch (Exception e) {
                log.error("Could not get pricing data from Pricing-Service for sku: {}", sku);
                log.error(e.getMessage());
                throw new NotFoundException("Could not get pricing data from Pricing-Service for sku: " + sku + " :: " + e.getMessage());
            }
        });
    }

    private <R> R executeRequest(Function<HttpHeaders, R> f) {
        try {
            return f.apply(getAuthHeaders());
        } catch (HttpClientErrorException e) {
            log.info("API call is failed for pricing-service: {}", e.getMessage());
            throw e;
        }
    }

    private synchronized HttpHeaders getAuthHeaders() {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", pricingServiceConfig.getApiKey());
        return headers;
    }
}
