package de.kfzteile24.salesOrderHub.dto.order;

import de.kfzteile24.salesOrderHub.dto.order.customer.SelfPickup;
import de.kfzteile24.salesOrderHub.dto.order.logisticalUnits.Item;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class LogisticalUnits {
    String shippingAddressKey;
    String contactSalutation;
    String contactFirstName;
    String contactLastName;
    String contactPhoneName;
    String contactEmail;
    String customerComment;
    SelfPickup selfPickup;
    String shippingAdvice;
    String shippingProvider;
    String ShippingType;
    Date expectedDeliveryDate;
    String expectedDeliveryMessage;
    String trackingNumber;
    String trackingLink;
    List<Item> logisticalItems;
}
