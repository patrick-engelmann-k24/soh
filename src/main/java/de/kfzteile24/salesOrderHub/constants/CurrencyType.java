package de.kfzteile24.salesOrderHub.constants;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor
public enum CurrencyType {
    EUR("EUR"),
    DKK("DKK");

    @NonNull
    private String name;
}
