package de.kfzteile24.salesOrderHub.dto.sns.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.soh.order.dto.BillingAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    @JsonProperty("Salutation")
    private String salutation;

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("LastName")
    private String lastName;

    @JsonProperty("Street")
    private String street;

    @JsonProperty("City")
    private String city;

    @JsonProperty("ZipCode")
    private String zipCode;

    @JsonProperty("CountryCode")
    private String countryCode;

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
