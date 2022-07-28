package de.kfzteile24.salesOrderHub.dto.sns.invoice;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreSalesInvoice {

    @JsonProperty("SalesInvoiceHeader")
    private CoreSalesInvoiceHeader salesInvoiceHeader;

    @JsonProperty("DeliveryNotes")
    private List<CoreDeliveryNote> deliveryNotes;


}
