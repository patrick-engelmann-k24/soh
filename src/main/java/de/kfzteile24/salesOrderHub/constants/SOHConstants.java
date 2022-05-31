package de.kfzteile24.salesOrderHub.constants;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class SOHConstants {

    public static final BigDecimal ONE_CENT = new BigDecimal("0.01");
    public static final String INVOICE_NUMBER_SEPARATOR = "-";
    public static final String ORDER_NUMBER_SEPARATOR = "-";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final String PATTERN_INVOICE_NUMBER = "^\\d{4}-1\\d{12}$";
    public static final int LENGTH_INVOICE_NUMBER = 18;
    public static final String PATTERN_CREDIT_NOTE_NUMBER = "^\\d{4}2\\d{5}$";
    public static final int LENGTH_CREDIT_NOTE_NUMBER = 10;
    public static final List<String> VIRTUAL_ITEMS_SKU = List.of("MARK-0001", "KBA", "KBA2", "KBA3");

    private SOHConstants() {}
}
