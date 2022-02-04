package de.kfzteile24.salesOrderHub.domain.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class OrderJsonConverterTest {
    @Mock
    private OrderJsonVersionDetector detector;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderJsonConverter orderJsonConverter;

    @Test
    public void anExceptionIsThrownIfTheVersionOfTheOrderJsonIsNotSupported() {

        assertThatThrownBy(() -> orderJsonConverter.convertToEntityAttribute(""))
                .isInstanceOf(IllegalStateException.class);
    }
}
