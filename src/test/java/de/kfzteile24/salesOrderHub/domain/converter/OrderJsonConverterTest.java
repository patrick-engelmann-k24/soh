package de.kfzteile24.salesOrderHub.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderJsonConverterTest {
    @Mock
    private OrderJsonVersionDetector detector;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderJsonConverter orderJsonConverter;

    @Test
    @SneakyThrows(JsonProcessingException.class)
    public void anExceptionIsThrownIfTheVersionOfTheOrderJsonIsNotSupported() {
        when(detector.isVersion2(any())).thenReturn(false);
        when(detector.isVersion3(any())).thenReturn(false);

        assertThatThrownBy(()-> orderJsonConverter.convertToEntityAttribute(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @SneakyThrows(JsonProcessingException.class)
    public void aVersion2OrderJsonIsDeserializedToTheVersion2DTOs() {
        when(detector.isVersion2(any())).thenReturn(true);
        when(objectMapper.readValue(anyString(), eq(OrderJSON.class))).thenReturn(new OrderJSON());

        final var orderJsonString = "some json";
        assertThat(orderJsonConverter.convertToEntityAttribute(orderJsonString)).isInstanceOf(OrderJSON.class);

        verify(objectMapper).readValue(eq(orderJsonString), eq(OrderJSON.class));

    }

}
