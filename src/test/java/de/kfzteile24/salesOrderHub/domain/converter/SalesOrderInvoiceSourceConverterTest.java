package de.kfzteile24.salesOrderHub.domain.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SalesOrderInvoiceSourceConverterTest {

    @InjectMocks
    private SalesOrderInvoiceSourceConverter salesOrderInvoiceSourceConverter;

    @Test
    void convertToEntityAttribute() {
        assertThat(salesOrderInvoiceSourceConverter.convertToEntityAttribute(null)).isNull();
        assertThat(salesOrderInvoiceSourceConverter.convertToEntityAttribute("")).isNull();
    }
}