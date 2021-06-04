package de.kfzteile24.salesOrderHub.dto.order.customer;

import lombok.Data;

@Data
public class Customer {
    private String customerNumber;
    private String customerDataHubId;
    private String customerId;
    private String customerType;
    private String customerEmail;
    private String contactId;
    private String contactEmail;
}
