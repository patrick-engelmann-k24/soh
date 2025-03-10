package de.kfzteile24.salesOrderHub.dto.sns.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreSalesInvoice {

    @NotNull
    @Valid
    @JsonProperty("SalesInvoiceHeader")
    private CoreSalesInvoiceHeader salesInvoiceHeader;

    @JsonProperty("DeliveryNotes")
    private List<CoreDeliveryNote> deliveryNotes;


}
