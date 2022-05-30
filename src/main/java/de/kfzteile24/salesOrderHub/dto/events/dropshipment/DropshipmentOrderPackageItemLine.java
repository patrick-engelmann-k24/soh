package de.kfzteile24.salesOrderHub.dto.events.dropshipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentOrderPackageItemLine {

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("quantity")
    private Integer quantity;
}
