package de.kfzteile24.salesOrderHub.dto.mapper;

import de.kfzteile24.salesOrderHub.domain.property.KeyValueProperty;
import de.kfzteile24.salesOrderHub.dto.property.PersistentProperty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KeyValuePropertyMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "value", expression = "java(persistentProperty.toString())")
    KeyValueProperty toKeyValueProperty(PersistentProperty persistentProperty);

    @Mapping(target = "value", source = "typedValue")
    PersistentProperty toPersistentProperty(KeyValueProperty keyValueProperty);

    default Object toPropertyValue(KeyValueProperty keyValueProperty) {
        return keyValueProperty.getTypedValue();
    }

    default String toKeyValuePropertyValue(PersistentProperty persistentProperty) {
        return persistentProperty.toString();
    }
}
