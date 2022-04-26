package de.kfzteile24.salesOrderHub.dto.sns.creditnote;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.deliverynote.CoreDeliveryNote;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;

@Builder
@Value
@Jacksonized
public class SalesCreditNote {

    @JsonProperty("SalesCreditNoteHeader")
    SalesCreditNoteHeader salesCreditNoteHeader;

    @JsonProperty("DeliveryNotes")
    Collection<CoreDeliveryNote> deliveryNotes;
}
