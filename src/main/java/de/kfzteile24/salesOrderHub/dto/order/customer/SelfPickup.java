package de.kfzteile24.salesOrderHub.dto.order.customer;

import lombok.Data;

@Data
public class SelfPickup {
    private String storeLocationCode;
    private String storeLocationName;
}
