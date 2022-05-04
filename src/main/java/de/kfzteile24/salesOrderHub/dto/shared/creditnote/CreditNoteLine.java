package de.kfzteile24.salesOrderHub.dto.shared.creditnote;

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

    @JsonProperty("UnitNetAmount")
    BigDecimal unitNetAmount;

    @JsonProperty("LineNetAmount")
    BigDecimal lineNetAmount;

    @JsonProperty("LineTaxAmount")
    BigDecimal lineTaxAmount;

    @JsonProperty("TaxRate")
    BigDecimal taxRate;

    @JsonProperty("IsShippingCost")
    Boolean isShippingCost;
}
