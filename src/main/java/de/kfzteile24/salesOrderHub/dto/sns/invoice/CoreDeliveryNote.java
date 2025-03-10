package de.kfzteile24.salesOrderHub.dto.sns.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

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
    private Date date;

    @JsonProperty("ShippingAddress")
    private Address shippingAddress;

    @JsonProperty("InventoryLocationId")
    private String inventoryLocationId;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("IsCancelled")
    private Boolean isCancelled;

    @JsonProperty("TrackingInformations")
    private List<CoreTrackingInformation> trackingInformations = null;

    @JsonProperty("DeliveryNoteLines")
    private List<CoreDeliveryNoteLine> deliveryNoteLines = null;


}
