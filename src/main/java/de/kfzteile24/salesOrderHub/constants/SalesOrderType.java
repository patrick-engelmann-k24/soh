package de.kfzteile24.salesOrderHub.constants;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor
public enum SalesOrderType {
    REGULAR("regular"),
    DROPSHIPMENT("dropshipment");

    @NonNull
    private String name;
}
