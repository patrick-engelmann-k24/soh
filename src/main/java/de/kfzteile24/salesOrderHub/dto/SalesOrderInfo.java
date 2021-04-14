package de.kfzteile24.salesOrderHub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author vinaya
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesOrderInfo {

  private OrderJSON order;
  private boolean recurringOrder;

}
