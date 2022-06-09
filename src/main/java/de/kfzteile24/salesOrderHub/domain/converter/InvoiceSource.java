package de.kfzteile24.salesOrderHub.domain.converter;

import lombok.Getter;

@Getter
public enum InvoiceSource {
    SOH("SOH");

    private String name;

    InvoiceSource(String name) {
        this.name = name;
    }
}
