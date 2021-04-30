package de.kfzteile24.salesOrderHub.dto.order;

import de.kfzteile24.salesOrderHub.dto.order.rows.Creator;
import de.kfzteile24.salesOrderHub.dto.order.rows.Item;
import de.kfzteile24.salesOrderHub.dto.order.rows.ItemInformation;
import de.kfzteile24.salesOrderHub.dto.order.rows.ItemNumbers;
import de.kfzteile24.salesOrderHub.dto.order.rows.ItemRules;
import de.kfzteile24.salesOrderHub.dto.order.rows.PartIdentificationProperties;
import de.kfzteile24.salesOrderHub.dto.order.rows.SumValues;
import de.kfzteile24.salesOrderHub.dto.order.rows.UnitValues;
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
    UnitValues unitValues;
    List<Item> items;
}
