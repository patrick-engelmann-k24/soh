package de.kfzteile24.salesOrderHub.services;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.LENGTH_CREDIT_NOTE_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.LENGTH_INVOICE_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.PATTERN_CREDIT_NOTE_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.PATTERN_INVOICE_NUMBER;

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
                    var invoiceNumber = docRefPart.substring(startPositionOfInvoiceNumber, dot);
                    if (invoiceNumber.matches(PATTERN_INVOICE_NUMBER)) {
                        return invoiceNumber;
                    }
                }
            }
        }

        throw new IllegalArgumentException("Cannot parse InvoiceNumber from invoice url: " + invoiceUrl);
    }

    public String extractCreditNoteNumber(final String invoiceUrl) {

        final var afterLastSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (afterLastSlash > 0) {
            final var docRefPart = invoiceUrl.substring(afterLastSlash);
            final int lastDashPosition = docRefPart.lastIndexOf("-") + 1;
            if (lastDashPosition > 0) {
                final var dot = docRefPart.indexOf('.', lastDashPosition);
                if (dot != -1) {
                    var creditNoteNumber = docRefPart.substring(lastDashPosition, dot);
                    if (creditNoteNumber.matches(PATTERN_CREDIT_NOTE_NUMBER)) {
                        return creditNoteNumber;
                    }
                }
            }
        }

        throw new IllegalArgumentException("Cannot parse InvoiceNumber from invoice url: " + invoiceUrl);
    }

    public String extractOrderNumber(final String invoiceUrl) {
        final var afterLastSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (afterLastSlash > 0) {
            final var numberPart = invoiceUrl.substring(afterLastSlash, invoiceUrl.lastIndexOf("."));
            if (numberPart.contains("-")) {
                int lastIndexOfOrderNumber;
                if (isDropshipmentCreditNote(invoiceUrl)) {
                    lastIndexOfOrderNumber = numberPart.length() - LENGTH_CREDIT_NOTE_NUMBER;
                } else {
                    lastIndexOfOrderNumber = numberPart.length() - LENGTH_INVOICE_NUMBER;
                }
                if (lastIndexOfOrderNumber > 0) {
                    return removeDashesFromEnd(numberPart.substring(0, lastIndexOfOrderNumber));
                }
            }
        }

        throw new IllegalArgumentException("Cannot parse OrderNumber from invoice url: " + invoiceUrl);
    }

    public boolean isDropshipmentCreditNote(final String invoiceUrl) {
        final var afterLastSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (afterLastSlash > 0) {
            final var afterOrderNumber = afterLastSlash + invoiceUrl.substring(afterLastSlash).lastIndexOf('-') + 1;
            if (afterOrderNumber > 0) {
                var invoiceNumber = invoiceUrl.substring(afterOrderNumber, invoiceUrl.indexOf(".", afterOrderNumber));
                return invoiceNumber.length() == 10 && invoiceNumber.matches(PATTERN_CREDIT_NOTE_NUMBER);
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

    private String removeDashesFromEnd(String number) {
        int lastIndex = number.length();
        for (int i = number.length() - 1; i >= 0 ; i--) {
            if (number.charAt(i) == '-') {
                lastIndex--;
            } else {
                return number.substring(0, lastIndex);
            }
        }
        return number;
    }

    public boolean isDropShipmentRelated(final String orderFulfillment) {
        return !StringUtils.equalsIgnoreCase(orderFulfillment, K24.getName());
    }
}
