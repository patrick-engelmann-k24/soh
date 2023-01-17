package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderBookedItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentPurchaseOrderBookedMessage {

    @NotNull
    @JsonProperty("PurchaseOrderNumber")
    private String purchaseOrderNumber;

    @NotNull
    @JsonProperty("SalesOrderNumber")
    private String salesOrderNumber;

    @JsonProperty("ExternalOrderNumber")
    private String externalOrderNumber;

    @NotNull
    @JsonProperty("SupplierNumber")
    private Integer supplierNumber;

    @JsonProperty("Items")
    private List<@NotNull DropshipmentPurchaseOrderBookedItem> items;

    @NotNull
    @JsonProperty("BookedDate")
    private LocalDateTime bookedDate;

    @NotNull
    @JsonProperty("Booked")
    private Boolean booked;

    @NotNull
    @JsonProperty("Message")
    private String message;
}
