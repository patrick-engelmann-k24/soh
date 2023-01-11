package de.kfzteile24.salesOrderHub.dto.dropshipment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentOrderShipped {

    private String orderNumber;

    private Collection<DropshipmentItemQuantity> items;
}
