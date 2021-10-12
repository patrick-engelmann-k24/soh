package de.kfzteile24.salesOrderHub.dto.events;

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
public class SalesOrderInfoEvent {

  private Object order;
  private boolean recurringOrder;

}
