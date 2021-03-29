package de.kfzteile24.salesOrderHub.dto.order.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    private String addressKey;
    private String addressFormat;
    private String addressType;
    private String company;
    private String salutation;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String street1;
    private String street2;
    private String street3;
    private String street4;
    private String city;
    private String zipCode;
    private String countryRegionCode;
    private String countryCode;
    private String taxNumber;
    private Boolean hasValidTaxNumber;
}

