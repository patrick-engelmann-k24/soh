package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.pdh.Product;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ItemSplitServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ItemSplitService itemSplitService;

    @Test
    @SneakyThrows
    void testGetProduct() {

        WireMockServer wireMockServer = new WireMockServer(18080);
        wireMockServer.start();

        wireMockServer.stubFor(WireMock.post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse().withBody("{ \"access_token\": \"fake_access_token\" }")));
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/json/v15/?sku=2270-13013"))
                .willReturn(aResponse().withBody(
                        Files.readAllBytes(Paths.get(Objects.requireNonNull(
                                getClass().getClassLoader().getResource("examples/product/2270-13013.json")).toURI())))));

        final var setSku = "2270-13013";
        Product product = itemSplitService.getProduct(setSku);
        assertNotNull(product);
        assertEquals(setSku, product.getSku());

        wireMockServer.stop();
    }

    @Test
    void testGetProductWhenPDHIsNotAvailable() {

        assertThatThrownBy(() -> itemSplitService.getProduct("2270-13013")).isInstanceOf(NotFoundException.class);
    }

}
