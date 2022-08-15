package de.kfzteile24.salesOrderHub.dto.shared.creditnote;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shared.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditNoteLine implements OrderItem {

    @JsonProperty("ItemNumber")
    private String itemNumber;

    @JsonProperty("Quantity")
    private BigDecimal quantity;

    @JsonProperty("UnitNetAmount")
    private BigDecimal unitNetAmount;

    @JsonProperty("LineNetAmount")
    private BigDecimal lineNetAmount;

    @JsonProperty("UnitGrossAmount")
    private BigDecimal unitGrossAmount;

    @JsonProperty("LineGrossAmount")
    private BigDecimal lineGrossAmount;

    @JsonProperty("LineTaxAmount")
    private BigDecimal lineTaxAmount;

    @JsonProperty("TaxRate")
    private BigDecimal taxRate;

    @JsonProperty("IsShippingCost")
    private Boolean isShippingCost;
}
