package de.kfzteile24.salesOrderHub.dto.sns.parcelshipped;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParcelShipped {

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("LastName")
    private String lastName;

    @JsonProperty("Street")
    private String street;

    @JsonProperty("ZipCode")
    private String zipCode;

    @JsonProperty("CountryCode")
    private String countryCode;

    @JsonProperty("City")
    private String city;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("OrderNumber")
    private String orderNumber;

    @JsonProperty("DeliveryNoteNumber")
    private String deliveryNoteNumber;

    @JsonProperty("TrackingNumber")
    private String trackingNumber;

    @JsonProperty("TrackingLink")
    private String trackingLink;

    @JsonProperty("AmountToBeCollected")
    private BigDecimal amountToBeCollected;

    @JsonProperty("ArticleItemsDtos")
    private Collection<ArticleItemsDto> articleItemsDtos;

    @JsonProperty("LogisticsPartnerName")
    private String logisticsPartnerName;
}