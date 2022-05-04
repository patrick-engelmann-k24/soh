package de.kfzteile24.salesOrderHub.dto.sns.deliverynote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Builder
@Value
@Jacksonized
public class CoreDeliveryNoteLine {

    @JsonProperty("ItemNumber")
    String itemNumber;

    @JsonProperty("Quantity")
    BigDecimal quantity;

    @JsonProperty("IsShippingCost")
    Boolean isShippingCost;
}
