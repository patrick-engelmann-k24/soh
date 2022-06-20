package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNote;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class SalesCreditNoteCreatedMessage {

    @JsonProperty("SalesCreditNote")
    SalesCreditNote salesCreditNote;

}
