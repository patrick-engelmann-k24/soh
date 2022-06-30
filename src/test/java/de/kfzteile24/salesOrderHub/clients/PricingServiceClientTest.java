package de.kfzteile24.salesOrderHub.clients;

import de.kfzteile24.salesOrderHub.configuration.PricingServiceConfig;
import de.kfzteile24.salesOrderHub.dto.pricing.Prices;
import de.kfzteile24.salesOrderHub.dto.pricing.PricingItem;
import de.kfzteile24.salesOrderHub.dto.pricing.SetUnitPriceAPIResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(MockitoExtension.class)
public class PricingServiceClientTest {
    private static final String PRICING_SERVICE_URL = "http://localhost:8080";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PricingServiceConfig pricingServiceConfig;

    @InjectMocks
    private PricingServiceClient pricingServiceClient;

    @BeforeEach
    public void before() {
        when(pricingServiceConfig.getUrl()).thenReturn(URI.create(PRICING_SERVICE_URL));
    }

    @Test
    public void theCorrectNetPriceIsReturnedIfThePricingServiceReturnsOne() {
        // mock pricing info answer
        final var expectedPricingInfo = new SetUnitPriceAPIResponse();
        expectedPricingInfo.setSetParentProductNumber("1010-0607");
        expectedPricingInfo.setSetUnitPrices(List.of(PricingItem.builder().sku("1010-0607").unitPrices(Prices.builder().gross(BigDecimal.valueOf(12.34)).build()).build()));
        mockSuccessfulPricingAnswer(expectedPricingInfo);

        final var pricingInfoOpt = pricingServiceClient.getSetPriceInfo("1010-0607", "www-k24-de", "12345");
        assertTrue(pricingInfoOpt.isPresent());
        assertEquals(expectedPricingInfo.getSetUnitPrices().get(0).getUnitPrices().getGross(),
                pricingInfoOpt.get().getSetUnitPrices().get(0).getUnitPrices().getGross());
    }

    @Test
    public void anEmptyOptionalIsReturnedIfThePricingServiceReturnsAnEmptyBody() {
        when(restTemplate.exchange(any(URI.class), any(), any(), ArgumentMatchers.<Class<SetUnitPriceAPIResponse>> any()))
                .thenReturn(new ResponseEntity<>(OK));

        final var pricingInfoOpt = pricingServiceClient.getSetPriceInfo("", "", "");
        assertFalse(pricingInfoOpt.isPresent());
    }

    private void mockSuccessfulPricingAnswer(final SetUnitPriceAPIResponse pricingInfo) {
        when(restTemplate.exchange(any(URI.class), any(), any(), ArgumentMatchers.<Class<SetUnitPriceAPIResponse>> any()))
                .thenReturn(new ResponseEntity<>(pricingInfo, OK));
    }
}
