package de.kfzteile24.salesOrderHub.dto.sns.creditnote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class BillingAddress {

    @JsonProperty("Salutation")
    String salutation;

    @JsonProperty("FirstName")
    String firstName;

    @JsonProperty("LastName")
    String lastName;

    @JsonProperty("Street")
    String street;

    @JsonProperty("City")
    String city;

    @JsonProperty("ZipCode")
    String zipCode;

    @JsonProperty("CountryCode")
    String countryCode;
}
