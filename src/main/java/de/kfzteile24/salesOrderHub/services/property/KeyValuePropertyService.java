package de.kfzteile24.salesOrderHub.services.property;

import de.kfzteile24.salesOrderHub.configuration.ProjectConfig;
import de.kfzteile24.salesOrderHub.domain.property.KeyValueProperty;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.repositories.KeyValuePropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyValuePropertyService {

    private final KeyValuePropertyRepository keyValuePropertyRepository;
    private final ProjectConfig projectConfig;

    @Transactional(readOnly = true)
    public Optional<KeyValueProperty> getPropertyByKey(String key) {
        return keyValuePropertyRepository.findByKey(key)
                .map(this::enrichWithTypedVale);
    }

    @Transactional(readOnly = true)
    public List<KeyValueProperty> getAllProperties() {
        return keyValuePropertyRepository.findAll().stream()
                .map(this::enrichWithTypedVale)
                .collect(Collectors.toUnmodifiableList());
    }

    @Transactional
    public KeyValueProperty save(KeyValueProperty keyValueProperty) {
        var savedKeyValueProperty = keyValuePropertyRepository.save(keyValueProperty);
        return enrichWithTypedVale(savedKeyValueProperty);
    }

    public void insertFromAppConfig() {
        projectConfig.getPersistentProperties().forEach(this::insertIfNotExists);
    }

    public void insertIfNotExists(ProjectConfig.PersistentPropertyConfig persistentPropertyConfig) {

        String key = persistentPropertyConfig.getKey();
        Object value = persistentPropertyConfig.getValue();

        getPropertyByKey(key)
                .ifPresentOrElse(keyValueProperty -> {
                            if (BooleanUtils.isTrue(persistentPropertyConfig.getOverwriteOnStartup())) {
                                keyValueProperty.setValue(value.toString());
                                keyValuePropertyRepository.save(keyValueProperty);
                                log.info("Existing property '{}' = '{}' saved successfully", key, value);
                            } else {
                                log.info("Property '{}' = '{}' already exists. Skip insert into the DB",
                                        keyValueProperty.getKey(), keyValueProperty.getValue());
                            }
                        },
                        () -> {
                            var keyValueProperty = KeyValueProperty.builder()
                                    .key(key)
                                    .value(value.toString())
                                    .build();
                            keyValuePropertyRepository.save(keyValueProperty);
                            log.info("New property '{}' = '{}' saved successfully", key, value);
                        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void fillKeyValuePropertyTable() {
        insertFromAppConfig();
    }

    private KeyValueProperty enrichWithTypedVale(KeyValueProperty keyValueProperty) {
        var typedValue = getTypedValue(keyValueProperty);
        keyValueProperty.setTypedValue(typedValue);
        return keyValueProperty;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private <T> T getTypedValue(KeyValueProperty keyValueProperty) {
        var key = keyValueProperty.getKey();
        var value = keyValueProperty.getValue();
        var property = projectConfig.getPersistentProperties().stream()
                .filter(p -> StringUtils.equalsIgnoreCase(p.getKey(), key))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Could not found persistent property. Key:  " + key));
        var propertyValueClazz = property.getValue().getClass();
        return (T) Optional.ofNullable(keyValueProperty.getValue())
                .map(v -> castToClass(value, propertyValueClazz))
                .orElse(null);

    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private <T> T castToClass(String value, Class<T> clazz) {
        if (Boolean.class.equals(clazz)) {
            return (T) Boolean.valueOf(value);
        } else if (Integer.class.equals(clazz)) {
            return (T) Integer.valueOf(value);
        } else if (Long.class.equals(clazz)) {
            return (T) Long.valueOf(value);
        } else if (Double.class.equals(clazz)) {
            return (T) Double.valueOf(value);
        } else {
            return (T) value;
        }
    }
}
