package de.kfzteile24.salesOrderHub.dto.sns.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoreDeliveryNoteLine {

    @JsonProperty("ItemNumber")
    private String itemNumber;

    @JsonProperty("Quantity")
    private BigDecimal quantity;

    @JsonProperty("IsShippingCost")
    private Boolean isShippingCost;


}
