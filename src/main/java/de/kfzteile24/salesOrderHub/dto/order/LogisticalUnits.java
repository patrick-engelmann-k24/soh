package de.kfzteile24.salesOrderHub.dto.order;

import de.kfzteile24.salesOrderHub.dto.order.customer.SelfPickup;
import de.kfzteile24.salesOrderHub.dto.order.logisticalUnits.Item;
import lombok.Data;

import java.util.List;

@Data
public class LogisticalUnits {
    private String shippingAddressKey;
    private String contactSalutation;
    private String contactFirstName;
    private String contactLastName;
    private String contactPhoneName;
    private String contactEmail;
    private String customerComment;
    private SelfPickup selfPickup;
    private String shippingAdvice;
    private String shippingProvider;
    private String shippingType;
    private String expectedDeliveryDate;
    private String expectedDeliveryMessage;
    private String trackingNumber;
    private String trackingLink;
    private List<Item> logisticalItems;
}
