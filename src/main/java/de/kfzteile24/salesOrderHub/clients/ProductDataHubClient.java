package de.kfzteile24.salesOrderHub.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ProductDataHubConfig;
import de.kfzteile24.salesOrderHub.domain.pdh.Product;
import de.kfzteile24.salesOrderHub.domain.pdh.ProductEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Function;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDataHubClient {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String GET_PDH_ENDPOINT = "/json/v15/?sku={sku}";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProductDataHubConfig productDataHubConfig;

    private String accessToken;

    public Product getProductBySku(String sku) {
        return executeRequest((authHeaders) -> {
            final var endpoint = new UriTemplate(productDataHubConfig.getPdhUrl() + GET_PDH_ENDPOINT).expand(sku);
            log.info("Calling product data hub endpoint: {}", endpoint);

            final var httpEntity = new HttpEntity<>(null, authHeaders);


            ResponseEntity<String> response = restTemplate.exchange(endpoint, GET, httpEntity, String.class);

            if (response.getStatusCode() == HttpStatus.FOUND) {
                URI location = response.getHeaders().getLocation();
                ResponseEntity<ProductEnvelope> productDataResponse =
                        restTemplate.exchange(Objects.requireNonNull(location), GET, null, ProductEnvelope.class);
                return Objects.requireNonNull(productDataResponse.getBody()).getProduct();
            } else {
                log.info("Could now get product data for sku: {}", sku);
                return null;
            }
        });
    }

    private <R> R executeRequest(Function<HttpHeaders, R> f) {
        try {
            return f.apply(getAuthHeaders(false));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.info("Authorization with token failed when calling pricing-service: {}", e.getMessage());
                return f.apply(getAuthHeaders(true));
            } else {
                throw e;
            }
        }
    }

    private synchronized HttpHeaders getAuthHeaders(boolean renew) {
        if (renew || accessToken == null) {
            accessToken = getAccessToken();
        }
        final var headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER_NAME, accessToken);
        return headers;
    }

    @SneakyThrows
    private String getAccessToken() {

        try {
            log.info("Fetching new token from cognito");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.add("Authorization", getAuthorizationHeader());

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

            final var response =
                    restTemplate.exchange(productDataHubConfig.getAuthUrl(), POST, entity, String.class);
            if (response.getBody() != null) {
                CognitoAuthResponse cognitoAuthResponse = objectMapper.readValue(response.getBody(), CognitoAuthResponse.class);
                return BEARER_PREFIX + cognitoAuthResponse.getAccessToken();
            } else {
                throw new IllegalArgumentException("Login to product-data-hun failed, could not get access token");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new IllegalArgumentException("Login to product-data-hun failed: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private String getAuthorizationHeader() {

        String toEncode = productDataHubConfig.getClientId() + ":" + productDataHubConfig.getClientSecret();
        return "Basic " + Base64.getUrlEncoder().encodeToString(toEncode.getBytes());
    }

}
