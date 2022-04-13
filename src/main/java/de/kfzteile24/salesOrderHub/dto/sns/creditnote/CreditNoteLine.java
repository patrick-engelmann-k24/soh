package de.kfzteile24.salesOrderHub.dto.sns.creditnote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Builder
@Value
@Jacksonized
public class CreditNoteLine {

    @JsonProperty("ItemNumber")
    String itemNumber;

    @JsonProperty("Quantity")
    BigDecimal quantity;

    @JsonProperty("LineNetAmount")
    BigDecimal lineNetAmount;

    @JsonProperty("LineTaxAmount")
    BigDecimal lineTaxAmount;

    @JsonProperty("IsShippingCost")
    Boolean isShippingCost;
}
