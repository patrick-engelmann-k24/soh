package de.kfzteile24.salesOrderHub.dto.sns.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreSalesInvoiceHeader {

    @NotBlank
    @JsonProperty("InvoiceNumber")
    private String invoiceNumber;

    @NotNull
    @JsonProperty("InvoiceDate")
    private Date invoiceDate;

    @NotEmpty
    @JsonProperty("InvoiceLines")
    private List<@NotNull @Valid CoreSalesFinancialDocumentLine> invoiceLines;

    @NotNull
    @JsonProperty("OrderGroupId")
    private String orderGroupId;

    @NotBlank
    @JsonProperty("OrderNumber")
    private String orderNumber;

    @NotNull
    @JsonProperty("CurrencyCode")
    private String currencyCode;

    @NotNull
    @JsonProperty("NetAmount")
    private Double netAmount;

    @NotNull
    @JsonProperty("GrossAmount")
    private Double grossAmount;

    @NotNull
    @Valid
    @JsonProperty("BillingAddress")
    private Address billingAddress;

}
