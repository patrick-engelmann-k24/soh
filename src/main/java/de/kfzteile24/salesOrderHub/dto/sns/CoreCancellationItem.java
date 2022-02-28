package de.kfzteile24.salesOrderHub.dto.sns;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreCancellationItem {

    private String sku;

    private Integer quantity;
}
