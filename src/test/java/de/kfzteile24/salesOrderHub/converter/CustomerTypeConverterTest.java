package de.kfzteile24.salesOrderHub.converter;

import de.kfzteile24.soh.order.dto.CustomerType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.UnusedPrivateMethod")
class CustomerTypeConverterTest {
    private final CustomerTypeConverter customerTypeConverter = new CustomerTypeConverter();

    @ParameterizedTest
    @MethodSource("provideCustomerTypeTestData")
    public void customerTypesAreConvertedCorrectly(String type, CustomerType expectedType) {
        assertThat(customerTypeConverter.convert(type)).isEqualTo(expectedType);
    }

    private static Stream<Arguments> provideCustomerTypeTestData() {
        return Stream.of(
                Arguments.of("guest", CustomerType.GUEST),
                Arguments.of("private", CustomerType.PRIVATE),
                Arguments.of("business", CustomerType.BUSINESS));
    }
}