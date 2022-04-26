package de.kfzteile24.salesOrderHub.dto.sns.deliverynote;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.salesOrderHub.dto.sns.shared.TrackingInformation;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.Collection;

@Builder
@Value
@Jacksonized
public class CoreDeliveryNote {

    @JsonProperty("OrderGroupId")
    String orderGroupId;

    @JsonProperty("OrderNumber")
    String orderNumber;

    @JsonProperty("Number")
    String number;

    @JsonProperty("Date")
    LocalDateTime date;

    @JsonProperty("ShippingAddress")
    Address shippingAddress;

    @JsonProperty("InventoryLocationId")
    String inventoryLocationId;

    @JsonProperty("Type")
    String type;

    @JsonProperty("IsCancelled")
    Boolean isCancelled;

    @JsonProperty("TrackingInformations")
    Collection<TrackingInformation> trackingInformations;

    @JsonProperty("DeliveryNoteLines")
    Collection<CoreDeliveryNoteLine> deliveryNoteLines;
}
