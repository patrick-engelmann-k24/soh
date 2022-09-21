package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author vinaya
 */
@Getter
@RequiredArgsConstructor
@NoArgsConstructor
public enum CustomerType {

  NEW("new"),
  RECURRING("recurring");

  @NonNull
  private String type;

}
