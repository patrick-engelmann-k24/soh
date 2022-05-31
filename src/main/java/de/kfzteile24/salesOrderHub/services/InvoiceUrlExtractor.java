package de.kfzteile24.salesOrderHub.services;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;

@UtilityClass
public class InvoiceUrlExtractor {

    public String extractInvoiceNumber(final String invoiceUrl) {

        final var afterLastSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (afterLastSlash > 0) {
            final var docRefPart = invoiceUrl.substring(afterLastSlash);
            final var startPositionOfInvoiceNumber = getStartPositionOfInvoiceNumber(docRefPart);

            if (startPositionOfInvoiceNumber > 0) {
                final var dot = docRefPart.indexOf('.', startPositionOfInvoiceNumber);
                if (dot != -1) {
                    return docRefPart.substring(startPositionOfInvoiceNumber, dot);
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

    private int getStartPositionOfInvoiceNumber(String docRefPart) {
        int lastDashPosition = docRefPart.lastIndexOf("-");
        int yearDigitCount = 4;
        if (lastDashPosition != -1) {
            var beforeYearInfo = lastDashPosition - yearDigitCount;
            return docRefPart.substring(0, beforeYearInfo).lastIndexOf("-") + 1;
        }
        return -1;
    }

    public boolean isDropShipmentRelated(final String orderFulfillment) {
        return !StringUtils.equalsIgnoreCase(orderFulfillment, K24.getName());
    }
}
