package de.kfzteile24.salesOrderHub.services.property;

import de.kfzteile24.salesOrderHub.configuration.ProjectConfig;
import de.kfzteile24.salesOrderHub.domain.property.KeyValueProperty;
import de.kfzteile24.salesOrderHub.repositories.KeyValuePropertyRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyValuePropertyServiceTest {

    private static final String ANY_PROPERTY_KEY = RandomStringUtils.randomAlphabetic(10);
    private static final String ANY_NOT_EXISTING_PROPERTY_KEY = RandomStringUtils.randomAlphabetic(10);
    private static final String ANY_PROPERTY_BOOLEAN_VALUE_TRUE = Boolean.TRUE.toString();
    private static final String ANY_PROPERTY_BOOLEAN_VALUE_FALSE = Boolean.FALSE.toString();
    private static final String ANY_CREATED_AT_STR = "2022-12-30T19:34:50.63";
    private static final String ANY_UPDATED_AT_STR = "2022-12-31T19:34:50.63";
    private static final LocalDateTime ANY_CREATED_AT = LocalDateTime.parse(ANY_CREATED_AT_STR);
    private static final LocalDateTime ANY_UPDATED_AT = LocalDateTime.parse(ANY_UPDATED_AT_STR);

    @InjectMocks
    @Spy
    private KeyValuePropertyService keyValuePropertyService;

    @Mock
    private KeyValuePropertyRepository keyValuePropertyRepository;

    @Mock
    private ProjectConfig projectConfig;

    @Test
    void testGetPropertyByKey() {
        var keyValueProperty = KeyValueProperty.builder()
                .key(ANY_PROPERTY_KEY)
                .value(ANY_PROPERTY_BOOLEAN_VALUE_TRUE)
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .build();

        var persistentPropertyConfig = ProjectConfig.PersistentPropertyConfig.builder()
                .key(ANY_PROPERTY_KEY)
                .value(true)
                .build();

        when(keyValuePropertyRepository.findByKey(anyString())).thenReturn(Optional.of(keyValueProperty));
        when(projectConfig.getPersistentProperties()).thenReturn(List.of(persistentPropertyConfig));

        var optKeyValueProperty = keyValuePropertyService.getPropertyByKey(ANY_PROPERTY_KEY);

        assertThat(optKeyValueProperty).isNotEmpty();

        keyValueProperty = optKeyValueProperty.get();

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(keyValueProperty.getKey()).isEqualTo(ANY_PROPERTY_KEY);
            softly.assertThat(keyValueProperty.getValue()).isEqualTo("true");
            softly.assertThat(keyValueProperty.getTypedValue()).isEqualTo(true);
        }

        verify(keyValuePropertyRepository).findByKey(ANY_PROPERTY_KEY);
    }

    @Test
    void testSave() {

        var keyValueProperty = KeyValueProperty.builder()
                .key(ANY_PROPERTY_KEY)
                .value(ANY_PROPERTY_BOOLEAN_VALUE_FALSE)
                .build();

        var persistentPropertyConfig = ProjectConfig.PersistentPropertyConfig.builder()
                .key(ANY_PROPERTY_KEY)
                .value(true)
                .build();

        when(keyValuePropertyRepository.save(any())).thenReturn(keyValueProperty);
        when(projectConfig.getPersistentProperties()).thenReturn(List.of(persistentPropertyConfig));

        keyValueProperty = keyValuePropertyService.save(keyValueProperty);

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(keyValueProperty.getKey()).isEqualTo(ANY_PROPERTY_KEY);
            softly.assertThat(keyValueProperty.getValue()).isEqualTo("false");
            softly.assertThat(keyValueProperty.getTypedValue()).isEqualTo(Boolean.FALSE);
        }

        verify(keyValuePropertyService).save(
                argThat(property -> {
                    assertThat(property.getId()).isNull();
                    assertThat(property.getKey()).isEqualTo(ANY_PROPERTY_KEY);
                    assertThat(property.getValue()).isEqualTo("false");
                    return true;
                })
        );
    }

    @Test
    void testInsertIfNotExists() {

        var keyValueProperty = KeyValueProperty.builder()
                .id(1L)
                .key(ANY_NOT_EXISTING_PROPERTY_KEY)
                .value(ANY_PROPERTY_BOOLEAN_VALUE_TRUE)
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .build();

        var persistentPropertyConfig = ProjectConfig.PersistentPropertyConfig.builder()
                .key(ANY_NOT_EXISTING_PROPERTY_KEY)
                .value(true)
                .overwriteOnStartup(true)
                .build();

        when(keyValuePropertyRepository.findByKey(anyString())).thenReturn(Optional.of(keyValueProperty));
        when(projectConfig.getPersistentProperties()).thenReturn(List.of(persistentPropertyConfig));

        keyValuePropertyService.insertIfNotExists(persistentPropertyConfig);

        verify(keyValuePropertyRepository).save(
                argThat(property -> {
                    assertThat(property.getId()).isEqualTo(1L);
                    assertThat(property.getKey()).isEqualTo(ANY_NOT_EXISTING_PROPERTY_KEY);
                    assertThat(property.getValue()).isEqualTo("true");
                    return true;
                }));
    }

    @Test
    void testInsertIfNotExists45() {

        var keyValueProperty = KeyValueProperty.builder()
                .key(ANY_PROPERTY_KEY)
                .value(ANY_PROPERTY_BOOLEAN_VALUE_TRUE)
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .build();

        var persistentPropertyConfig = ProjectConfig.PersistentPropertyConfig.builder()
                .key(ANY_PROPERTY_KEY)
                .value(true)
                .overwriteOnStartup(false)
                .build();

        when(keyValuePropertyRepository.findByKey(anyString())).thenReturn(Optional.of(keyValueProperty));
        when(projectConfig.getPersistentProperties()).thenReturn(List.of(persistentPropertyConfig));

        keyValuePropertyService.insertIfNotExists(persistentPropertyConfig);

        verify(keyValuePropertyRepository, never()).save(keyValueProperty);
    }

    @Test
    void testInsertIfNotExists2() {

        var keyValueProperty = KeyValueProperty.builder()
                .key(ANY_PROPERTY_KEY)
                .value(ANY_PROPERTY_BOOLEAN_VALUE_TRUE)
                .build();

        when(keyValuePropertyRepository.findByKey(anyString())).thenReturn(Optional.empty());

        var persistentPropertyConfig = ProjectConfig.PersistentPropertyConfig.builder()
                .key(ANY_PROPERTY_KEY)
                .value(true)
                .overwriteOnStartup(true)
                .build();

        keyValuePropertyService.insertIfNotExists(persistentPropertyConfig);

        verify(keyValuePropertyRepository).save(keyValueProperty);
    }
}