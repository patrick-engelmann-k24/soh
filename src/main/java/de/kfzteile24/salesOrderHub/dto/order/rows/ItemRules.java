package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

import java.util.List;

@Data
public class ItemRules {
    private String identifier;
    private List<String> relatedSkus;
}
