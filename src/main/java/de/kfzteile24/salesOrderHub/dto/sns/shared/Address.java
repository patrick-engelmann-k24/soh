package de.kfzteile24.salesOrderHub.dto.sns.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.soh.order.dto.BillingAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    @JsonProperty("Salutation")
    private String salutation;

    @NotNull
    @JsonProperty("FirstName")
    private String firstName;

    @NotNull
    @JsonProperty("LastName")
    private String lastName;

    @NotNull
    @JsonProperty("Street")
    private String street;

    @NotNull
    @JsonProperty("City")
    private String city;

    @NotNull
    @JsonProperty("ZipCode")
    private String zipCode;

    @NotBlank
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
