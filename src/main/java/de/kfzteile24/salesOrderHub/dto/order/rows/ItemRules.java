package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

import java.util.List;

@Data
public class ItemRules {
    String identifier;
    List<String> relatedSkus;
}
