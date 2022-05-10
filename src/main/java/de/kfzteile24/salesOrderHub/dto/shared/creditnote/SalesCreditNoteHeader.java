package de.kfzteile24.salesOrderHub.dto.shared.creditnote;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.constants.CurrencyType;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

@Builder
@Value
@Jacksonized
public class SalesCreditNoteHeader {

    @JsonProperty("OrderGroupId")
    String orderGroupId;

    @JsonProperty("OrderNumber")
    String orderNumber;

    @JsonProperty("CreditNoteNumber")
    String creditNoteNumber;

    @JsonProperty("CreditNoteDate")
    LocalDateTime creditNoteDate;

    @JsonProperty("CurrencyCode")
    CurrencyType currencyCode;

    @JsonProperty("NetAmount")
    BigDecimal netAmount;

    @JsonProperty("GrossAmount")
    BigDecimal grossAmount;

    @JsonProperty("BillingAddress")
    Address billingAddress;

    @JsonProperty("CreditNoteLines")
    Collection<CreditNoteLine> creditNoteLines;
}
