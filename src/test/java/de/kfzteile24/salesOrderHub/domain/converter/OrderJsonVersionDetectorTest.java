package de.kfzteile24.salesOrderHub.domain.converter;

import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("PMD.UnusedPrivateMethod")
class OrderJsonVersionDetectorTest {
    private final OrderJsonVersionDetector detector =
            new OrderJsonVersionDetector(new ObjectMapperConfig().objectMapper());

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("provideVersionDetectionTestData")
    public void theCorrectVersionIsDetected(String orderJson, String expectedVersion, boolean isVersion2) {
        assertThat(detector.detectVersion(orderJson)).isEqualTo(expectedVersion);
        assertThat(detector.isVersion2(orderJson)).isEqualTo(isVersion2);
        assertThat(detector.isVersion3(orderJson)).isEqualTo(!isVersion2);
    }

    @Test
    public void anExceptionIfThrownIfTheJsonContainsNoVersion() {
        assertThatThrownBy(() -> detector.detectVersion("{\"some_prop\": 1}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> provideVersionDetectionTestData() {
        return Stream.of(
                Arguments.of("{\"version\": \"2.1\", \"some_prop\": 1}", "2.1", true),
                Arguments.of("{\"version\": \"2.2\", \"some_prop\": 1}", "2.2", true),
                Arguments.of("{\"version\": \"3.0\", \"some_prop\": 1}", "3.0", false),
                Arguments.of("{\"version\": \"3.1\", \"some_prop\": 1}", "3.1", false)
        );
    }
}