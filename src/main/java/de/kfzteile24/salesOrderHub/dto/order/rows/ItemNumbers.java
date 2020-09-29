package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

import java.util.List;

@Data
public class ItemNumbers {
    List<String> ean;
    String dataSupplierNumber;
    String manufacturerProductNumber;
}
