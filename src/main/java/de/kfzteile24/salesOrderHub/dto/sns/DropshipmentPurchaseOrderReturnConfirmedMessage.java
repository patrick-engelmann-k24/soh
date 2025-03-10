package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderPackage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentPurchaseOrderReturnConfirmedMessage {

    @NotNull
    @JsonProperty("SalesOrderNumber")
    private String salesOrderNumber;

    @NotNull
    @JsonProperty("ExternalOrderNumber")
    private String externalOrderNumber;

    @NotEmpty
    @JsonProperty("Packages")
    private List<@NotNull @Valid DropshipmentPurchaseOrderPackage> packages;

}
