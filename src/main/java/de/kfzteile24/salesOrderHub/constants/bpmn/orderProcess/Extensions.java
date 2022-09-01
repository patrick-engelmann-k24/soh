package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Extensions  implements BpmItem {

    NEW_RELIC_EVENT("newRelicEvent"),
    TEMPORAL_UNIT("temporalUnit");

    @Getter
    private final String name;
}
