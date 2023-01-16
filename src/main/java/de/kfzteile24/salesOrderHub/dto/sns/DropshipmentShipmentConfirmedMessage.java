package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DropshipmentShipmentConfirmedMessage {

    @NotNull
    @JsonProperty("SupplierInternalId")
    private Integer supplierInternalId;

    @NotNull
    @JsonProperty("PurchaseOrderNumber")
    private String purchaseOrderNumber;

    @NotNull
    @JsonProperty("SalesOrderNumber")
    private String salesOrderNumber;

    @NotNull
    @JsonProperty("ShipmentDate")
    private String shipmentDate;

    @NotEmpty
    @JsonProperty("Items")
    private Collection<@NotNull ShipmentItem> items;
}
