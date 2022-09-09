package de.kfzteile24.salesOrderHub.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class InvoiceDocument {

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("content")
    private String content;
}
