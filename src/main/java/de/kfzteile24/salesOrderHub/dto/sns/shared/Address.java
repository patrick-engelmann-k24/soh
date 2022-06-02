package de.kfzteile24.salesOrderHub.dto.sns.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.soh.order.dto.BillingAddress;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class Address {

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

    public static Address fromBillingAddress(BillingAddress billingAddress) {
        if (billingAddress == null) {
            return null;
        }
        return Address.builder()
                .city(billingAddress.getCity())
                .countryCode(billingAddress.getCountryCode())
                .firstName(billingAddress.getFirstName())
                .lastName(billingAddress.getLastName())
                .salutation(billingAddress.getSalutation())
                .street(getStreet(billingAddress))
                .zipCode(billingAddress.getZipCode())
                .build();
    }

    public static String getStreet(BillingAddress address) {
        return address.getStreet1() +
                (address.getStreet2() != null ? " " + address.getStreet2() : "") +
                (address.getStreet3() != null ? " " + address.getStreet3() : "");
    }
}
