package de.kfzteile24.salesOrderHub.dto.dropshipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DropshipmentOrderShipped {

    @JsonProperty("order_number")
    private String orderNumber;

    private Collection<DropshipmentItemQuantity> items;
}
