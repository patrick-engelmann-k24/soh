package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum EventType {

    SIGNAL("signal"),
    MESSAGE("message");

    @Getter
    private final String name;
}
