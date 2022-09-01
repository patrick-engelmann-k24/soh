package de.kfzteile24.salesOrderHub.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CustomerSegment {
    B2B("b2b"),
    DIRECT_DELIVERY("direct_delivery");

    @Getter
    private final String name;
}
