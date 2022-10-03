package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.paypal.PaypalEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaypalRefundInstructionSuccessfulEvent {

    @JsonProperty("event")
    private PaypalEvent event;
}
