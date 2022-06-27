package de.kfzteile24.salesOrderHub.dto.sns.parcelshipped;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Builder
@Value
@Jacksonized
public class ArticleItemsDto {

    @JsonProperty("Number")
    String number;

    @JsonProperty("Description")
    String description;

    @JsonProperty("Quantity")
    BigDecimal quantity;

    @JsonProperty("IsDeposit")
    boolean isDeposit;
}