package de.kfzteile24.salesOrderHub.services;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class InvoiceUrlExtractor {

    public String extractInvoiceNumber(final String invoiceUrl) {

        final var afterLastSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (afterLastSlash > 0) {
            final var docRefPart = invoiceUrl.substring(afterLastSlash);
            final var afterLastMinus = docRefPart.lastIndexOf("-") + 1;

            if (afterLastMinus > 0) {
                final var dot = docRefPart.indexOf('.', afterLastMinus);
                if (dot != -1) {
                    return docRefPart.substring(afterLastMinus, dot);
                }
            }
        }

        throw new IllegalArgumentException("Cannot parse InvoiceNumber from invoice url: " + invoiceUrl);
    }

    public String extractOrderNumber(final String invoiceUrl) {
        final var afterLastSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (afterLastSlash > 0) {
            final var minus = invoiceUrl.indexOf("-", afterLastSlash);
            if (minus != -1) {
                return invoiceUrl.substring(afterLastSlash, minus);
            }
        }

        throw new IllegalArgumentException("Cannot parse OrderNumber from invoice url: " + invoiceUrl);
    }

    public boolean isDropShipmentRelated(final String invoiceUrl) {
        return Pattern.compile("s3://.+/dropshipment/.+").matcher(invoiceUrl).matches();
    }
}
