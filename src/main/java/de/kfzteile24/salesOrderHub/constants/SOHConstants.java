package de.kfzteile24.salesOrderHub.constants;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public final class SOHConstants {

    public static final BigDecimal TWO_CENTS = new BigDecimal("0.02");
    public static final String INVOICE_NUMBER_SEPARATOR = "-";
    public static final String ORDER_NUMBER_SEPARATOR = "-";

    public static final String CREDIT_NOTE_NUMBER_SEPARATOR = "2";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final String PATTERN_INVOICE_NUMBER = "^\\d{4}-1\\d{12}$";
    public static final int LENGTH_INVOICE_NUMBER = 18;
    public static final String PATTERN_CREDIT_NOTE_NUMBER = "^\\d{4}2\\d{5}$";
    public static final int LENGTH_CREDIT_NOTE_NUMBER = 10;
    public static final int LENGTH_YEAR_DIGIT = 4;
    public static final String COMBINED_ITEM_SEPARATOR = ",";
    public static final String TRACE_ID_NAME = "x-business-key";
    public static final String REQUEST_ID_KEY = "RequestID";
    public static final String RETURN_ORDER_NUMBER_PREFIX = "RO";

    private SOHConstants() {}
}
