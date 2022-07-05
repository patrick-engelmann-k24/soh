package de.kfzteile24.salesOrderHub.dto.sns.parcelshipped;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;

@Builder
@Value
@Jacksonized
public class ParcelShippedMessage {

    @JsonProperty("MessageType")
    Collection<String> messageType;

    @JsonProperty("Message")
    ParcelShipped message;
}
