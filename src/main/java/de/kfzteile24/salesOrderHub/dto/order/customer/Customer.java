package de.kfzteile24.salesOrderHub.dto.order.customer;

import lombok.Data;

@Data
public class Customer {
    String customerNumber;
    String customerId;
    String customerType;
    String customerEmail;
    String contactId;
    String contactEmail;
}
