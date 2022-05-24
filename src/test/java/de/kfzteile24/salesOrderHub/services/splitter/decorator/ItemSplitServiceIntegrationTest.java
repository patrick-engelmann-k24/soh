package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ItemSplitServiceIntegrationTest {

    @Autowired
    private ItemSplitService itemSplitService;

    @Test
    @SneakyThrows
    void processOrderList() {

        WireMockServer wireMockServer = new WireMockServer(18080);
        wireMockServer.start();

        wireMockServer.stubFor(WireMock.post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse().withBody("{ \"access_token\": \"fake_access_token\" }")));
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/json/v15/?sku=2270-13013"))
                .willReturn(aResponse().withBody(
                        Files.readAllBytes(Paths.get(Objects.requireNonNull(
                                getClass().getClassLoader().getResource("examples/product/2270-13013.json")).toURI())))));

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/json/v15/?sku=DZN"))
                .willReturn(aResponse().withBody(
                        Files.readAllBytes(Paths.get(Objects.requireNonNull(
                                getClass().getClassLoader().getResource("examples/product/DZN.json")).toURI())))));

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/json/v15/?sku=1410-4610"))
                .willReturn(aResponse().withBody(
                        Files.readAllBytes(Paths.get(Objects.requireNonNull(
                                getClass().getClassLoader().getResource("examples/product/DZN.json")).toURI())))));

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/json/v15/?sku=1420-4355"))
                .willReturn(aResponse().withBody(
                        Files.readAllBytes(Paths.get(Objects.requireNonNull(
                                getClass().getClassLoader().getResource("examples/product/DZN.json")).toURI())))));


        final var setSku = "2270-13013";
        final var setItemSku = "DZN";

        final var order1 = getOrder(readResource("examples/splitterSalesOrderMessageWithOneSetRows.json"));
        final var list = new ArrayList<Order>();
        list.add(order1);

        itemSplitService.processOrderList(list);

        for (Order order : list) {
            final var rows = order.getOrderRows();

            // check if setItem is NOT in the list
            assertThat(getCountForSku(rows, setSku)).isEqualTo(0);

            assertThat(getCountForSku(rows, setItemSku)).isEqualTo(2);
            final var firstReplacementItem = findRowBySku(rows);
            assertThat(firstReplacementItem).isNotNull();
            assertThat(firstReplacementItem.getQuantity()).isEqualTo(BigDecimal.valueOf(1));
            assertThat(firstReplacementItem.getRowKey()).isEqualTo(2);

        }

        wireMockServer.stop();
    }

    @Test
    void processOrderWhenPDHIsNotAvailable() {

        final var setSku = "2270-13013";
        final var setItemSku = "DZN";

        final var order1 = getOrder(readResource("examples/splitterSalesOrderMessageWithOneSetRows.json"));
        final var list = new ArrayList<Order>();
        list.add(order1);

        itemSplitService.processOrderList(list);

        for (Order order : list) {
            final var rows = order.getOrderRows();

            // check if setItem is NOT in the list
            assertThat(getCountForSku(rows, setSku)).isEqualTo(1);
            assertThat(getCountForSku(rows, setItemSku)).isEqualTo(0);

        }
    }

    protected int getCountForSku(List<OrderRows> rows, final String sku) {
        return (int) rows.stream().filter(r -> Objects.equals(r.getSku(), sku)).count();
    }

    protected OrderRows findRowBySku(List<OrderRows> rows) {
        return rows.stream().filter(r -> Objects.equals(r.getSku(), "DZN")).findFirst().orElseThrow();
    }

}
