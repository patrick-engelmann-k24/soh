package de.kfzteile24.salesOrderHub.constants;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CustomerType {

  NEW("new"),
  RECURRING("recurring");

  @NonNull
  private final String type;

}
