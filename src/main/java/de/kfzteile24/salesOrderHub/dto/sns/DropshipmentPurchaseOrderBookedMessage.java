package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderBookedItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentPurchaseOrderBookedMessage {

    @JsonProperty("PurchaseOrderNumber")
    private String purchaseOrderNumber;

    @JsonProperty("SalesOrderNumber")
    private String salesOrderNumber;

    @JsonProperty("SupplierNumber")
    private Integer supplierNumber;

    @JsonProperty("Items")
    private List<DropshipmentPurchaseOrderBookedItem> items;

    @JsonProperty("BookedDate")
    private LocalDateTime bookedDate;

    @JsonProperty("Booked")
    private Boolean booked;

    @JsonProperty("Message")
    private String message;
}
