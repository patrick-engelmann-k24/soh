package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

import java.util.List;

@Data
public class ItemNumbers {
    private List<String> ean;
    private String dataSupplierNumber;
    private String manufacturerProductNumber;
}
