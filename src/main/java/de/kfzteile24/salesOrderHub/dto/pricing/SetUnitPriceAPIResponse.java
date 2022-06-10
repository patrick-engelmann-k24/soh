package de.kfzteile24.salesOrderHub.dto.pricing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class SetUnitPriceAPIResponse {

    @JsonProperty("set_parent_product_number")
    private String setParentProductNumber;

    @JsonProperty("sales_channel_code")
    private String salesChannelCode;

    @JsonProperty("set_unit_prices")
    List<PricingItem> setUnitPrices;
}
