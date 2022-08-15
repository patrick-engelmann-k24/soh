package de.kfzteile24.salesOrderHub.dto.shared.creditnote;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesCreditNoteHeader {

    @JsonProperty("OrderGroupId")
    private String orderGroupId;

    @JsonProperty("OrderNumber")
    private String orderNumber;

    @JsonProperty("CreditNoteNumber")
    private String creditNoteNumber;

    @JsonProperty("CreditNoteDate")
    private LocalDateTime creditNoteDate;

    @JsonProperty("CurrencyCode")
    private String currencyCode;

    @JsonProperty("NetAmount")
    private BigDecimal netAmount;

    @JsonProperty("GrossAmount")
    private BigDecimal grossAmount;

    @JsonProperty("BillingAddress")
    private Address billingAddress;

    @JsonProperty("CreditNoteLines")
    private Collection<CreditNoteLine> creditNoteLines;
}
