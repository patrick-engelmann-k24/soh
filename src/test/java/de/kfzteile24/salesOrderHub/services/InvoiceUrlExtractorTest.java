package de.kfzteile24.salesOrderHub.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class InvoiceUrlExtractorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "xxxxxxxxx-xxxx-xxxxxxxxx",
        "xxxxxxxxx--xxxx-xxxxxxxxx",
        "xxxxxxxxx-------------------xxxx-xxxxxxxxx"
    })
    void testExtractInvoiceNumber(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        var invoiceNumber = InvoiceUrlExtractor.extractInvoiceNumber(invoiceUrl);
        assertThat(invoiceNumber).isEqualTo("xxxx-xxxxxxxxx");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "xxxxxxxxx-xxxx-xxxxxxxxxxxxx",
            "xxxxxxxxx--xxxx-xxxxxxxxxxxxx",
            "xxxxxxxxx-------------------xxxx-xxxxxxxxxxxxx"
    })
    void testExtractOrderNumber(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        var orderNumber = InvoiceUrlExtractor.extractOrderNumber(invoiceUrl);
        assertThat(orderNumber).isEqualTo("xxxxxxxxx");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "xxxxxxxxxxxxxxxxxx",
            "xxxxxxxxx--xxxxxxxxx",
    })
    void testExtractInvoiceNumberThrownException(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        assertThatThrownBy(() -> InvoiceUrlExtractor.extractInvoiceNumber(invoiceUrl))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse InvoiceNumber");
    }


    @Test
    void testExtractOrderNumberThrownException() {
        var invoiceUrl = "s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/xxxxxxxxxxxxxxxxxx.pdf";
        assertThatThrownBy(() -> InvoiceUrlExtractor.extractOrderNumber(invoiceUrl))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse OrderNumber");
    }

    @Test
    void testIsDropShipmentRelatedTrue() {
        assertThat(InvoiceUrlExtractor.isDropShipmentRelated("delticom")).isTrue();
    }

    @Test
    void testIsDropShipmentRelatedFalse() {
        assertThat(InvoiceUrlExtractor.isDropShipmentRelated("K24")).isFalse();
    }
}