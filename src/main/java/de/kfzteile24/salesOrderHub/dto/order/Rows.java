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
    private String position;
    private String rowKey;
    private String sku;
    private Number quantity;
    private String quantityUnitType;
    private PartIdentificationProperties partIdentificationProperties;
    private ItemNumbers itemNumbers;
    private ItemInformation itemInformation;
    private List<ItemRules> itemRules;
    private Creator creator;
    private Taxes tax;
    private SumValues sumValues;
    private UnitValues unitValues;
    private List<Item> items;
}
