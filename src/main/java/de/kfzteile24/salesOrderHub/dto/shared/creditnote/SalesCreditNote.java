package de.kfzteile24.salesOrderHub.dto.shared.creditnote;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.deliverynote.CoreDeliveryNote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesCreditNote {

    @NotNull
    @JsonProperty("SalesCreditNoteHeader")
    private SalesCreditNoteHeader salesCreditNoteHeader;

    @JsonProperty("DeliveryNotes")
    private Collection<CoreDeliveryNote> deliveryNotes;
}
