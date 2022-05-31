package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderPackage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentPurchaseOrderReturnNotifiedMessage {

    @JsonProperty("ExternalOrderNumber")
    private String externalOrderNumber;

    @JsonProperty("SalesOrderNumber")
    private String salesOrderNumber;

    @JsonProperty("Packages")
    private List<DropshipmentPurchaseOrderPackage> packages;
}
