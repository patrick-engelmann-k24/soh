package de.kfzteile24.salesOrderHub.dto.sns.creditnote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class SalesCreditNote {

    @JsonProperty("SalesCreditNoteHeader")
    SalesCreditNoteHeader salesCreditNoteHeader;
}
