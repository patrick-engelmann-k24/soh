package de.kfzteile24.salesOrderHub.dto.sns.invoice;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.creditnote.BillingAddress;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoreSalesInvoiceHeader {

    @JsonProperty("InvoiceNumber")
    private String invoiceNumber;

    @JsonProperty("InvoiceDate")
    private Date invoiceDate;

    @JsonProperty("InvoiceLines")
    private List<CoreSalesFinancialDocumentLine> invoiceLines;

    @JsonProperty("OrderGroupId")
    private String orderGroupId;

    @JsonProperty("OrderNumber")
    private String orderNumber;

    @JsonProperty("CurrencyCode")
    private String currencyCode;

    @JsonProperty("NetAmount")
    private Double netAmount;

    @JsonProperty("GrossAmount")
    private Double grossAmount;

    @JsonProperty("BillingAddress")
    private BillingAddress billingAddress;

}
