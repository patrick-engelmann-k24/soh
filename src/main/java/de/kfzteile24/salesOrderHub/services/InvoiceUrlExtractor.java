package de.kfzteile24.salesOrderHub.services;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.LENGTH_CREDIT_NOTE_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.LENGTH_INVOICE_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.LENGTH_YEAR_DIGIT;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.PATTERN_CREDIT_NOTE_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.PATTERN_INVOICE_NUMBER;

@UtilityClass
public class InvoiceUrlExtractor {

    public String extractInvoiceNumber(final String invoiceUrl) {

        final var lastIndexOfSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (lastIndexOfSlash > 0) {

            if (matchesInvoiceNumberPattern(invoiceUrl)) {
                var invoiceNumber = getYearPartBeforeLastDash(invoiceUrl) + "-" + getAfterLastDash(invoiceUrl);
                if (invoiceNumber.matches(PATTERN_INVOICE_NUMBER)) {
                    return addLeadingDashIfNeeded(invoiceUrl, invoiceNumber);
                }
            } else {
                var invoiceNumber = getAfterLastDash(invoiceUrl);
                if (!invoiceNumber.isEmpty()) {
                    return addLeadingDashIfNeeded(invoiceUrl, invoiceNumber);
                }
            }
        }

        throw new IllegalArgumentException("Cannot parse InvoiceNumber from invoice url: " + invoiceUrl);
    }

    private String addLeadingDashIfNeeded(String invoiceUrl, String invoiceNumber) {
        String invoiceUrlWithoutFileExtension = invoiceUrl.substring(0, invoiceUrl.lastIndexOf("."));
        return invoiceUrlWithoutFileExtension.endsWith("--" + invoiceNumber) ? ("-" + invoiceNumber) : invoiceNumber;
    }

    public String extractCreditNoteNumber(final String invoiceUrl) {
        final var lastIndexOfSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (lastIndexOfSlash > 0) {
            var creditNoteNumber = getAfterLastDash(invoiceUrl);
            if (creditNoteNumber.matches(PATTERN_CREDIT_NOTE_NUMBER)) {
                return creditNoteNumber;
            }
        }

        throw new IllegalArgumentException("Cannot parse CreditNoteNumber from invoice url: " + invoiceUrl);
    }

    public String extractOrderNumber(final String invoiceUrl) {
        final var lastIndexOfSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (lastIndexOfSlash > 0) {
            final var numberPart = invoiceUrl.substring(lastIndexOfSlash, invoiceUrl.lastIndexOf("."));
            if (numberPart.contains("-")) {
                int lastIndexOfOrderNumber;
                if (matchesCreditNoteNumberPattern(invoiceUrl)) {
                    lastIndexOfOrderNumber = numberPart.length() - LENGTH_CREDIT_NOTE_NUMBER;
                } else if (matchesInvoiceNumberPattern(invoiceUrl)) {
                    lastIndexOfOrderNumber = numberPart.length() - LENGTH_INVOICE_NUMBER;
                } else {
                    lastIndexOfOrderNumber = numberPart.lastIndexOf("-");
                }
                if (lastIndexOfOrderNumber > 0) {
                    return removeDashesFromEnd(numberPart.substring(0, lastIndexOfOrderNumber));
                }
            }
        }

        throw new IllegalArgumentException("Cannot parse OrderNumber from invoice url: " + invoiceUrl);
    }

    public String extractReturnOrderNumber(final String invoiceUrl) {

        final var lastIndexOfSlash = invoiceUrl.lastIndexOf('/') + 1;

        if (lastIndexOfSlash > 0) {
            return invoiceUrl.substring(lastIndexOfSlash, invoiceUrl.lastIndexOf("."));
        } else {
            throw new IllegalArgumentException("Cannot parse filename from invoice url: " + invoiceUrl);
        }
    }

    private static String getAfterLastDash(String source) {
        final var lastIndexOfSlash = source.lastIndexOf('/') + 1;

        if (lastIndexOfSlash > 0) {
            final var lastIndexOfDash = source.lastIndexOf('-') + 1;
            if (lastIndexOfDash > 0 && lastIndexOfDash > lastIndexOfSlash) {
                return source.substring(lastIndexOfDash, source.indexOf(".", lastIndexOfDash));
            }
        }
        return "";
    }

    private static String getYearPartBeforeLastDash(String source) {
        final var lastIndexOfDash = source.lastIndexOf('-');
        if (lastIndexOfDash > 0) {
            final var startIndex = lastIndexOfDash - LENGTH_YEAR_DIGIT;
            if (checkIfDashExistsBefore(source, startIndex)) {
                return source.substring(startIndex, lastIndexOfDash);
            }
        }
        return "";
    }

    private static boolean checkIfDashExistsBefore(String source, int startIndex) {
        return source.charAt(startIndex - 1) == '-';
    }

    private static String removeDashesFromEnd(String number) {
        int lastIndex = number.length();
        for (int i = number.length() - 1; i >= 0; i--) {
            if (number.charAt(i) == '-') {
                lastIndex--;
            } else {
                return number.substring(0, lastIndex);
            }
        }
        return number;
    }

    private static boolean matchesInvoiceNumberPattern(String invoiceUrl) {
        final var lastIndexOfSlash = invoiceUrl.lastIndexOf('/') + 1;
        if (lastIndexOfSlash > 0) {
            var invoiceNumber = getYearPartBeforeLastDash(invoiceUrl) + "-" + getAfterLastDash(invoiceUrl);
            return invoiceNumber.matches(PATTERN_INVOICE_NUMBER);
        }
        return false;
    }

    public boolean matchesCreditNoteNumberPattern(final String invoiceUrl) {
        final var lastIndexOfSlash = invoiceUrl.lastIndexOf('/') + 1;
        if (lastIndexOfSlash > 0) {
            var invoiceNumber = getAfterLastDash(invoiceUrl);
            return invoiceNumber.matches(PATTERN_CREDIT_NOTE_NUMBER);
        }
        return false;
    }

    public boolean isDropShipmentRelated(final String orderFulfillment) {
        return orderFulfillment != null && !StringUtils.equalsIgnoreCase(orderFulfillment, K24.getName());
    }
}
