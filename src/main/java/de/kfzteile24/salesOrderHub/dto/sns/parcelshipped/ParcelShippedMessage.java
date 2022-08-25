package de.kfzteile24.salesOrderHub.dto.sns.parcelshipped;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParcelShippedMessage {

    @JsonProperty("MessageType")
    private Collection<String> messageType;

    @JsonProperty("Message")
    private ParcelShipped message;
}
