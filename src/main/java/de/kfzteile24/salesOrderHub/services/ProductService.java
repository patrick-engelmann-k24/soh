package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.domain.pdh.Product;
import de.kfzteile24.salesOrderHub.domain.pdh.ProductEnvelope;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ProductService {

    final String pdhAPI;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper mapper;

    final String PDH_PATH = "json/v15/";

    public ProductService(@Value("${kfzteile.pdh.apiEndpoint}") String pdhAPI) {
        this.pdhAPI = pdhAPI;
    }

    public Product getProductBySku(String sku) {

        try {
            WebClient.UriSpec<WebClient.RequestBodySpec> uriSpec = webClient.method(HttpMethod.GET);
            final WebClient.RequestBodySpec bodySpec = uriSpec.uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host(pdhAPI)
                    .path(PDH_PATH)
                    .queryParam("sku", sku)
                    .build());

            Mono<String> response = bodySpec.retrieve().bodyToMono(String.class);
            final var productEnvelope = json2Product(response.block());
            return productEnvelope.getProduct();
        } catch (Exception e) {
            log.info("Could not get product data from PDH for sku: {}", sku);
            return null;
        }
    }

    @SneakyThrows
    protected ProductEnvelope json2Product(String json) {
        return mapper.readValue(json, ProductEnvelope.class);
    }
}
