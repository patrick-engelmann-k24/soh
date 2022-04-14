package de.kfzteile24.salesOrderHub.domain.converter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor
public enum InvoiceSource {
    SOH("SOH");

    @NonNull
    private String name;
}
