package de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class TrackingLink {

    private String url;

    @Singular
    @JsonProperty("order_items")
    private Collection<String> orderItems;

}
