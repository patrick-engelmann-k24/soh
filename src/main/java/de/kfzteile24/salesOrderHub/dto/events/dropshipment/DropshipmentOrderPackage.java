package de.kfzteile24.salesOrderHub.dto.events.dropshipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentOrderPackage {

    @JsonProperty("tracking_link")
    String trackingLink;

    @JsonProperty("items")
    private List<DropshipmentOrderPackageItemLine> items;
}
