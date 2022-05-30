package de.kfzteile24.salesOrderHub.helper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum EventType {

    SIGNAL("signal"),
    MESSAGE("message");

    @Getter
    private final String name;
}
