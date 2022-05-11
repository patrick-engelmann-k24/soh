package de.kfzteile24.salesOrderHub.dto.sns.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shared.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreSalesFinancialDocumentLine implements OrderItem {

    @JsonProperty("ItemNumber")
    private String itemNumber;

    @JsonProperty("Quantity")
    private BigDecimal quantity;

    @JsonProperty("UnitNetAmount")
    private BigDecimal unitNetAmount;

    @JsonProperty("LineNetAmount")
    private BigDecimal lineNetAmount;

    @JsonProperty("LineTaxAmount")
    private BigDecimal lineTaxAmount;

    @JsonProperty("TaxRate")
    private BigDecimal taxRate;

    @JsonProperty("IsShippingCost")
    private Boolean isShippingCost;

}
