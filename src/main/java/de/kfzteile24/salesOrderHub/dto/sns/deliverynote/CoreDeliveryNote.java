package de.kfzteile24.salesOrderHub.dto.sns.deliverynote;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.salesOrderHub.dto.sns.shared.TrackingInformation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoreDeliveryNote {

    @JsonProperty("OrderGroupId")
    private String orderGroupId;

    @JsonProperty("OrderNumber")
    private String orderNumber;

    @JsonProperty("Number")
    private String number;

    @JsonProperty("Date")
    private LocalDateTime date;

    @JsonProperty("ShippingAddress")
    private Address shippingAddress;

    @JsonProperty("InventoryLocationId")
    private String inventoryLocationId;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("IsCancelled")
    private Boolean isCancelled;

    @JsonProperty("TrackingInformations")
    private Collection<TrackingInformation> trackingInformations;

    @JsonProperty("DeliveryNoteLines")
    private Collection<CoreDeliveryNoteLine> deliveryNoteLines;
}
