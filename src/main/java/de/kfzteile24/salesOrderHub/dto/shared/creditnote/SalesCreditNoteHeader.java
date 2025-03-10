package de.kfzteile24.salesOrderHub.dto.shared.creditnote;

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

    @NotBlank
    @JsonProperty("OrderNumber")
    private String orderNumber;

    @NotBlank
    @JsonProperty("CreditNoteNumber")
    private String creditNoteNumber;

    @NotNull
    @JsonProperty("CreditNoteDate")
    private LocalDateTime creditNoteDate;

    @NotNull
    @JsonProperty("CurrencyCode")
    private String currencyCode;

    @NotNull
    @JsonProperty("NetAmount")
    private BigDecimal netAmount;

    @NotNull
    @JsonProperty("GrossAmount")
    private BigDecimal grossAmount;

    @NotNull
    @Valid
    @JsonProperty("BillingAddress")
    private Address billingAddress;

    @NotEmpty
    @JsonProperty("CreditNoteLines")
    private Collection<@NotNull @Valid CreditNoteLine> creditNoteLines;
}
