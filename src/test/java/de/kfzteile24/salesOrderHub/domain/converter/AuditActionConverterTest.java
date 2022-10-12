package de.kfzteile24.salesOrderHub.domain.converter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import de.kfzteile24.salesOrderHub.domain.audit.Action;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AuditActionConverterTest {

    private final AuditActionConverter auditActionConverter = new AuditActionConverter();

    @ParameterizedTest
    @MethodSource("provideParamsToConvertActionsToStrings")
    void convertToString(Action action, String expectedValue) {
        assertThat(auditActionConverter.convertToDatabaseColumn(action)).isEqualTo(expectedValue);
    }

    @ParameterizedTest
    @MethodSource("provideParamsToConvertStringsToActions")
    void convertToAction(String value, Action expectedAction) {
        assertThat(auditActionConverter.convertToEntityAttribute(value)).isEqualTo(expectedAction);
    }

    @Test
    void tryingToConvertANullActionThrowsAnException() {
        assertThatThrownBy(() -> auditActionConverter.convertToDatabaseColumn(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("audit action");
    }

    @Test
    void tryingToConvertAnUnknownActionStringThrowsAnException() {
        assertThatThrownBy(() -> auditActionConverter.convertToEntityAttribute("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> provideParamsToConvertActionsToStrings() {
        return Arrays.stream(Action.values())
                .map(e -> Arguments.of(e, e.getAction()));
    }

    private static Stream<Arguments> provideParamsToConvertStringsToActions() {
        return Arrays.stream(Action.values())
                .map(e -> Arguments.of(e.getAction(), e));
    }
}