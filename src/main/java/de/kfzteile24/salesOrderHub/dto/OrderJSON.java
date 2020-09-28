package de.kfzteile24.salesOrderHub.dto;

import de.kfzteile24.salesOrderHub.dto.order.Header;
import de.kfzteile24.salesOrderHub.dto.order.LogisticalUnits;
import de.kfzteile24.salesOrderHub.dto.order.Rows;
import lombok.Data;

import java.util.List;

@Data
public class OrderJSON {
    String version;
    Header orderHeader;
    List<Rows> orderRows;
    List<LogisticalUnits> logisticalUnits;
}
