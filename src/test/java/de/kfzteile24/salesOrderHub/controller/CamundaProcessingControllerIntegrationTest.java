package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.constants.PersistentProperties;
import de.kfzteile24.salesOrderHub.domain.property.KeyValueProperty;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CamundaProcessingControllerIntegrationTest extends AbstractControllerIntegrationTest {

    private static final String ANY_PROPERTY_KEY = RandomStringUtils.randomAlphabetic(10);
    private static final String ANY_PROPERTY_VALUE = RandomStringUtils.randomAlphabetic(10);
    private static final String ANY_PROPERTY_KEY_2 = RandomStringUtils.randomAlphabetic(10);
    private static final String ANY_PROPERTY_VALUE_2 = RandomStringUtils.randomAlphabetic(10);
    private static final String ANY_CREATED_AT_STR = "2022-12-30T19:34:50.63";
    private static final String ANY_UPDATED_AT_STR = "2022-12-31T19:34:50.63";
    private static final LocalDateTime ANY_CREATED_AT = LocalDateTime.parse(ANY_CREATED_AT_STR);
    private static final LocalDateTime ANY_UPDATED_AT = LocalDateTime.parse(ANY_UPDATED_AT_STR);

    @MockBean
    private KeyValuePropertyService keyValuePropertyService;

    @MockBean
    private DropshipmentOrderService dropshipmentOrderService;

    @Test
    void testStorePersistentProperty() throws Exception {

        var existingKeyValueProperty = KeyValueProperty.builder()
                .key(ANY_PROPERTY_KEY)
                .value(ANY_PROPERTY_VALUE_2)
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .typedValue(ANY_PROPERTY_VALUE_2)
                .build();

        var modifiedKeyValueProperty = KeyValueProperty.builder()
                .key(ANY_PROPERTY_KEY)
                .value(ANY_PROPERTY_VALUE_2)
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .typedValue(ANY_PROPERTY_VALUE_2)
                .build();

        when(keyValuePropertyService.getPropertyByKey(anyString())).thenReturn(Optional.of(existingKeyValueProperty));
        when(keyValuePropertyService.save(any())).thenReturn(modifiedKeyValueProperty);

        mvc.perform(put("/camunda-processing/property")
                .param("key", ANY_PROPERTY_KEY)
                .param("value", ANY_PROPERTY_VALUE_2)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(ANY_PROPERTY_KEY))
                .andExpect(jsonPath("$.value").value(ANY_PROPERTY_VALUE_2))
                .andExpect(jsonPath("$.created_at").value(ANY_CREATED_AT_STR))
                .andExpect(jsonPath("$.updated_at").value(ANY_UPDATED_AT_STR));

        verify(keyValuePropertyService).getPropertyByKey(ANY_PROPERTY_KEY);
        verify(keyValuePropertyService).save(modifiedKeyValueProperty);
    }

    @Test
    void testGetPersistentProperty() throws Exception {

        var keyValueProperty = KeyValueProperty.builder()
                .key(ANY_PROPERTY_KEY)
                .value(ANY_PROPERTY_VALUE)
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .typedValue(ANY_PROPERTY_VALUE)
                .build();

        when(keyValuePropertyService.getPropertyByKey(anyString())).thenReturn(Optional.of(keyValueProperty));

        mvc.perform(get("/camunda-processing/property/{key}", ANY_PROPERTY_KEY)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(ANY_PROPERTY_KEY))
                .andExpect(jsonPath("$.value").value(ANY_PROPERTY_VALUE))
                .andExpect(jsonPath("$.created_at").value(ANY_CREATED_AT_STR))
                .andExpect(jsonPath("$.updated_at").value(ANY_UPDATED_AT_STR));

        verify(keyValuePropertyService).getPropertyByKey(ANY_PROPERTY_KEY);
    }

    @Test
    void testGetAllPersistentProperties() throws Exception {

        var keyValueProperty1 = KeyValueProperty.builder()
                .key(ANY_PROPERTY_KEY)
                .value(ANY_PROPERTY_VALUE)
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .typedValue(ANY_PROPERTY_VALUE)
                .build();

        var keyValueProperty2 = KeyValueProperty.builder()
                .key(ANY_PROPERTY_KEY_2)
                .value(ANY_PROPERTY_VALUE_2)
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .typedValue(ANY_PROPERTY_VALUE_2)
                .build();

        when(keyValuePropertyService.getAllProperties()).thenReturn(List.of(keyValueProperty1, keyValueProperty2));

        mvc.perform(get("/camunda-processing/property")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value(ANY_PROPERTY_KEY))
                .andExpect(jsonPath("$[0].value").value(ANY_PROPERTY_VALUE))
                .andExpect(jsonPath("$[0].created_at").value(ANY_CREATED_AT_STR))
                .andExpect(jsonPath("$[0].updated_at").value(ANY_UPDATED_AT_STR))
                .andExpect(jsonPath("$[1].key").value(ANY_PROPERTY_KEY_2))
                .andExpect(jsonPath("$[1].value").value(ANY_PROPERTY_VALUE_2))
                .andExpect(jsonPath("$[1].created_at").value(ANY_CREATED_AT_STR))
                .andExpect(jsonPath("$[1].updated_at").value(ANY_UPDATED_AT_STR));

        verify(keyValuePropertyService).getAllProperties();
    }

    @Test
    void testHandleProcessingDropshipmentStateTrue() throws Exception {

        var savedKeyValueProperty = KeyValueProperty.builder()
                .key(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING)
                .value(Boolean.TRUE.toString())
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .typedValue(Boolean.TRUE)
                .build();

        when(dropshipmentOrderService.setPauseDropshipmentProcessing(true)).thenReturn(savedKeyValueProperty);

        mvc.perform(put("/camunda-processing/pause/dropshipment/{pauseDropshipmentProcessing}", true)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING))
                .andExpect(jsonPath("$.value").value(Boolean.TRUE.toString()))
                .andExpect(jsonPath("$.created_at").value(ANY_CREATED_AT_STR))
                .andExpect(jsonPath("$.updated_at").value(ANY_UPDATED_AT_STR));

        verify(dropshipmentOrderService).setPauseDropshipmentProcessing(true);
    }

    @Test
    void testHandleProcessingDropshipmentStateFalse() throws Exception {

        var savedKeyValueProperty = KeyValueProperty.builder()
                .key(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING)
                .value(Boolean.FALSE.toString())
                .createdAt(ANY_CREATED_AT)
                .updatedAt(ANY_UPDATED_AT)
                .typedValue(Boolean.FALSE)
                .build();

        when(dropshipmentOrderService.setPauseDropshipmentProcessing(false)).thenReturn(savedKeyValueProperty);

        mvc.perform(put("/camunda-processing/pause/dropshipment/{pauseDropshipmentProcessing}", false)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING))
                .andExpect(jsonPath("$.value").value(Boolean.FALSE.toString()))
                .andExpect(jsonPath("$.created_at").value(ANY_CREATED_AT_STR))
                .andExpect(jsonPath("$.updated_at").value(ANY_UPDATED_AT_STR));

        verify(dropshipmentOrderService).setPauseDropshipmentProcessing(false);
    }
}