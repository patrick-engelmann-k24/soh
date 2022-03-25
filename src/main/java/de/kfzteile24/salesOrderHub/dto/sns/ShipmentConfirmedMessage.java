package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;

@Builder
@Value
@Jacksonized
public class ShipmentConfirmedMessage {

    @JsonProperty("SupplierInternalId")
    Integer supplierInternalId;

    @JsonProperty("PurchaseOrderNumber")
    String purchaseOrderNumber;

    @JsonProperty("SalesOrderNumber")
    String salesOrderNumber;

    @JsonProperty("ServiceProviderName")
    String serviceProviderName;

    @JsonProperty("ShipmentDate")
    String shipmentDate;

    @JsonProperty("Items")
    Collection<ShipmentItem> items;
}
