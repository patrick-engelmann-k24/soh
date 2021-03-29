package de.kfzteile24.salesOrderHub.dto;

import de.kfzteile24.salesOrderHub.dto.order.Header;
import de.kfzteile24.salesOrderHub.dto.order.LogisticalUnits;
import de.kfzteile24.salesOrderHub.dto.order.Rows;
import java.util.List;
import lombok.Data;

@Data
public class OrderJSON {
    String version;
    Header orderHeader;
    List<Rows> orderRows;
    List<LogisticalUnits> logisticalUnits;
}
