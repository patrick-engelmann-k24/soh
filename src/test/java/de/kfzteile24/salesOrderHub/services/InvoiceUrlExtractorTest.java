package de.kfzteile24.salesOrderHub.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class InvoiceUrlExtractorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "514073754-710108946",
        "514073754--710108946",
        "514073754-------------------710108946"
    })
    void testExtractInvoiceNumber(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        var invoiceNumber = InvoiceUrlExtractor.extractInvoiceNumber(invoiceUrl);
        assertThat(invoiceNumber).isEqualTo("710108946");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "514073754-710108946",
            "514073754--710108946",
            "514073754-------------------710108946"
    })
    void testExtractOrderNumber(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        var orderNumber = InvoiceUrlExtractor.extractOrderNumber(invoiceUrl);
        assertThat(orderNumber).isEqualTo("514073754");
    }

    @Test
    void testExtractInvoiceNumberThrownException() {
        var invoiceUrl = "s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/514073754710108946.pdf";
        assertThatThrownBy(() -> InvoiceUrlExtractor.extractInvoiceNumber(invoiceUrl))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse InvoiceNumber");
    }


    @Test
    void testExtractOrderNumberThrownException() {
        var invoiceUrl = "s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/514073754710108946.pdf";
        assertThatThrownBy(() -> InvoiceUrlExtractor.extractOrderNumber(invoiceUrl))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse OrderNumber");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "s3://production-k24-invoices/dropshipment/2021/06/04/514073754-710108946.pdf",
            "s3://staging-k24-invoices/dropshipment//2021/06/04/514073754---710108946.pdf.pdf",
            "s3://anyBucket/dropshipment/2021/06/04/514073754-710108946.pdf"
    })
    void testIsDropShipmentRelatedTrue(String invoiceUrl) {
        assertThat(InvoiceUrlExtractor.isDropShipmentRelated(invoiceUrl)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "s3://production-k24-invoices/dropshipment1/2021/06/04/514073754-710108946.pdf",
            "s3://staging-k24-invoices/dropshipment2/2021/06/04/514073754-710108946.pdf",
            "s3://anyBucket/anyFolder/2021/06/04/514073754-710108946.pdf",
            "s3://anyBucket/dropshipment/",
            "s3:///dropshipment/2021/06/04/514073754-710108946.pdf"
    })
    void testIsDropShipmentRelatedFalse(String invoiceUrl) {
        assertThat(InvoiceUrlExtractor.isDropShipmentRelated(invoiceUrl)).isFalse();
    }
}