package de.kfzteile24.salesOrderHub.dto.sns.parcelshipped;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.Collection;

@Builder
@Value
@Jacksonized
public class ParcelShipped {

    @JsonProperty("FirstName")
    String firstName;

    @JsonProperty("LastName")
    String lastName;

    @JsonProperty("Street")
    String street;

    @JsonProperty("ZipCode")
    String zipCode;

    @JsonProperty("CountryCode")
    String countryCode;

    @JsonProperty("City")
    String city;

    @JsonProperty("Email")
    String email;

    @JsonProperty("OrderNumber")
    String orderNumber;

    @JsonProperty("TrackingNumber")
    String trackingNumber;

    @JsonProperty("TrackingLink")
    String trackingLink;

    @JsonProperty("AmountToBeCollected")
    BigDecimal amountToBeCollected;

    @JsonProperty("ArticleItemsDtos")
    Collection<ArticleItemsDto> articleItemsDtos;

    @JsonProperty("LogisticsPartnerName")
    String logisticsPartnerName;
}