package de.kfzteile24.salesOrderHub.dto.order.customer;

import lombok.Data;

@Data
public class SelfPickup {
    String storeLocationCode;
    String storeLocationName;
}
