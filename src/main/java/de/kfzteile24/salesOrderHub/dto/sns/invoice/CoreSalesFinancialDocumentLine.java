package de.kfzteile24.salesOrderHub.dto.sns.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shared.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreSalesFinancialDocumentLine implements OrderItem {

    @NotBlank
    @JsonProperty("ItemNumber")
    private String itemNumber;

    @NotNull
    @JsonProperty("Quantity")
    private BigDecimal quantity;

    @NotNull
    @JsonProperty("UnitNetAmount")
    private BigDecimal unitNetAmount;

    @NotNull
    @JsonProperty("LineNetAmount")
    private BigDecimal lineNetAmount;

    @NotNull
    @JsonProperty("UnitGrossAmount")
    private BigDecimal unitGrossAmount;

    @NotNull
    @JsonProperty("LineGrossAmount")
    private BigDecimal lineGrossAmount;

    @NotNull
    @JsonProperty("LineTaxAmount")
    private BigDecimal lineTaxAmount;

    @NotNull
    @JsonProperty("TaxRate")
    private BigDecimal taxRate;

    @NotNull
    @JsonProperty("IsShippingCost")
    private Boolean isShippingCost;

    @NotBlank
    @JsonProperty("Description")
    private String description;

}
