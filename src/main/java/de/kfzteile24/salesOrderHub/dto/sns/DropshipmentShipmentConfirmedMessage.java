package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DropshipmentShipmentConfirmedMessage {

    @JsonProperty("SupplierInternalId")
    private Integer supplierInternalId;

    @JsonProperty("PurchaseOrderNumber")
    private String purchaseOrderNumber;

    @JsonProperty("SalesOrderNumber")
    private String salesOrderNumber;

    @JsonProperty("ShipmentDate")
    private String shipmentDate;

    @JsonProperty("Items")
    private Collection<ShipmentItem> items;
}
