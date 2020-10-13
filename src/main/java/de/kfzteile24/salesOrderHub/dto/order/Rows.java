package de.kfzteile24.salesOrderHub.dto.order;

import de.kfzteile24.salesOrderHub.dto.order.rows.*;
import de.kfzteile24.salesOrderHub.dto.order.total.Taxes;
import lombok.Data;

import java.util.List;

@Data
public class Rows {
    String position;
    String rowKey;
    String sku;
    Number quantity;
    String quantityUnitType;
    PartIdentificationProperties partIdentificationProperties;
    ItemNumbers itemNumbers;
    ItemInformation itemInformation;
    List<ItemRules> itemRules;
    Creator creator;
    Taxes tax;
    SumValues sumValues;
    List<SumValues> items;
}
