package de.kfzteile24.salesOrderHub.dto.sns.parcelshipped;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticleItemsDto {

    @JsonProperty("Number")
    private String number;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Quantity")
    private BigDecimal quantity;

    @JsonProperty("IsDeposit")
    private boolean isDeposit;
}