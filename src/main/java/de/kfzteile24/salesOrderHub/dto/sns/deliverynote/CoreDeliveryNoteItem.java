package de.kfzteile24.salesOrderHub.dto.sns.deliverynote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Builder
@Value
@Jacksonized
public class CoreDeliveryNoteItem {

    @JsonProperty("Sku")
    String sku;

    @JsonProperty("Quantity")
    BigDecimal quantity;

    @JsonProperty("UnitPriceGross")
    BigDecimal unitPriceGross;

    @JsonProperty("SalesPriceGross")
    BigDecimal salesPriceGross;

    @JsonProperty("TaxRate")
    BigDecimal taxRate;
}
