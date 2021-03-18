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
    private UUID orderId;
    private String orderNumber;
    private Date orderDatetime;
    private String orderTimezone;
    private String orderCurrency;
    private String orderReferenceId;
    private String orderReferenceOrderNumber;
    private String orderReferenceReason;
    private String offerId;
    private String offerReferenceNumber;
    private Origin origin;
    private Creator creator;
    private List<Discount> discounts;
    private List<Payment> payments;
    private Address billingAddress;
    private List<Address> shippingAddresses;
}
