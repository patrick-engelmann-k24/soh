package de.kfzteile24.salesOrderHub.dto.dropshipment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentItemQuantity {

    private String sku;

    private Integer quantity;
}
