package de.kfzteile24.salesOrderHub.dto.order;

import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.dto.order.header.Creator;
import de.kfzteile24.salesOrderHub.dto.order.header.Discount;
import de.kfzteile24.salesOrderHub.dto.order.header.Origin;
import de.kfzteile24.salesOrderHub.dto.order.header.Payment;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class Header {
    UUID orderId;
    String orderNumber;
    Date orderDatetime;
    String orderTimezone;
    String orderCurrency;
    String orderReferenceId;
    String orderReferenceOrderNumber;
    String orderReferenceReason;
    String offerId;
    String offerReferenceNumber;
    Origin origin;
    Creator creator;
    List<Discount> discounts;
    List<Payment> payments;
    Address billingAddress;
    List<Address> shippingAddress;
}
