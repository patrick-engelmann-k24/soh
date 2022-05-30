package de.kfzteile24.salesOrderHub.constants;

import java.time.format.DateTimeFormatter;

import java.math.BigDecimal;

public final class SOHConstants {

    public static final BigDecimal ONE_CENT = new BigDecimal("0.01");
    public static final String INVOICE_NUMBER_SEPARATOR = "-";
    public static final String ORDER_NUMBER_SEPARATOR = "-";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private SOHConstants() {}
}
