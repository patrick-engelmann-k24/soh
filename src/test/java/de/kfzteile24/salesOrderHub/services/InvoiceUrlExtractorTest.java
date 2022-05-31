package de.kfzteile24.salesOrderHub.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class InvoiceUrlExtractorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "987654321-1234-1123456789012",
        "987654321-1-1234-1123456789012",
        "987654321-900009-1234-1123456789012",
        "987654321--1234-1123456789012",
        "987654321-------------------1234-1123456789012"
    })
    void testExtractInvoiceNumber(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        var invoiceNumber = InvoiceUrlExtractor.extractInvoiceNumber(invoiceUrl);
        assertThat(invoiceNumber).isEqualTo("1234-1123456789012");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "987654321-2022212345",
        "987654321-1-2022212345",
        "987654321-900009-2022212345",
        "987654321--2022212345",
        "987654321-------------------2022212345"
    })
    void testExtractCreditNoteNumber(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        var creditNoteNumber = InvoiceUrlExtractor.extractCreditNoteNumber(invoiceUrl);
        assertThat(creditNoteNumber).isEqualTo("2022212345");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "987654321-1234-1123456789012",
            "987654321--1234-1123456789012",
            "987654321-2022212345",
            "987654321--2022212345",
            "987654321-------------------1234-112345678901"
    })
    void testExtractOrderNumber(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        var orderNumber = InvoiceUrlExtractor.extractOrderNumber(invoiceUrl);
        assertThat(orderNumber).isEqualTo("987654321");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "987654321-12345-1234-1123456789012",
            "987654321-12345--1234-1123456789012",
            "987654321-12345-2022212345",
            "987654321-12345--2022212345"
    })
    void testExtractOrderNumber2(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        var orderNumber = InvoiceUrlExtractor.extractOrderNumber(invoiceUrl);
        assertThat(orderNumber).isEqualTo("987654321-12345");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456789123456789",
            "987654321--123456789",
            "987654321-x-2022212345",
    })
    void testExtractInvoiceNumberThrownException(String docRefPart) {
        var invoiceUrl = String.format("s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", docRefPart);
        assertThatThrownBy(() -> InvoiceUrlExtractor.extractInvoiceNumber(invoiceUrl))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse InvoiceNumber");
    }


    @Test
    void testExtractOrderNumberThrownException() {
        var invoiceUrl = "s3://production-k24-invoices/app_android-kfzteile24-de/2021/06/04/123456789098754321.pdf";
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