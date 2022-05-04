package de.kfzteile24.salesOrderHub.dto.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreSalesInvoiceCreatedReceivedEvent {

    @JsonProperty("SalesInvoice")
    private CoreSalesInvoice salesInvoice;
}
