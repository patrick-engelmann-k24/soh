package de.kfzteile24.salesOrderHub.domain.converter;

import de.kfzteile24.salesOrderHub.domain.audit.Action;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Objects;
import java.util.stream.Stream;

@Converter
public class AuditActionConverter implements AttributeConverter<Action, String> {
    @Override
    public String convertToDatabaseColumn(Action action) {
        Objects.requireNonNull(action, "The audit action must not be null");
        return action.getAction();
    }

    @Override
    public Action convertToEntityAttribute(String action) {
        return Stream.of(Action.values())
                .filter(c -> c.getAction().equals(action))
                .findAny()
                .orElseThrow(IllegalArgumentException::new);
    }
}
