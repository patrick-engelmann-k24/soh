package de.kfzteile24.salesOrderHub.dto.order.customer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Address {
    String addressKey;
    String addressFormat;
    String addressType;
    String company;
    String salutation;
    String firstName;
    String lastName;
    String phoneNumber;
    String street1;
    String street2;
    String street3;
    String street4;
    String city;
    String zipCode;
    String countryRegionCode;
    String countryCode;
    String taxNumber;
    Boolean hasValidTaxNumber;
}
