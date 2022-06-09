package de.kfzteile24.salesOrderHub.clients;

import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PricingServiceClientIntegrationTest {

    @Autowired
    private PricingServiceClient pricingServiceClient;

    @Test
    @Ignore("This test is used only for manuel check of pricing service connection on staging environment")
    void testPricingServiceConnection() {
        var pricingInfo = pricingServiceClient.getSetPriceInfo("1010-0607", "www-k24-de");
        assertThat(pricingInfo).isPresent();
        assertThat(pricingInfo.get().getSetParentProductNumber()).isEqualTo("1010-0607");
        assertThat(pricingInfo.get().getSetUnitPrices().get(0).getUnitPrices().getGross()).isNotEqualTo(8.49);
        assertThat(pricingInfo.get().getSetUnitPrices().get(0).getUnitPrices().getNet()).isNotEqualTo(7.13);
    }
}
