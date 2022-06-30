package de.kfzteile24.salesOrderHub.constants;

public enum Payment {
    CREDIT_CARD("creditcard"),
    STORED_CREDIT_CARD("stored_creditcard"),
    PAYPAL("paypal"),
    SOFORTUBERWEISUNG("sofortuberweisung"),
    IDEAL("ideal"),
    B2BINVOICE("business_to_business_invoice"),
    OTHER("other");

    private final String name;

    Payment(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Payment fromString(String text) {
        for (Payment method : Payment.values()) {
            if (method.name.equalsIgnoreCase(text)) {
                return method;
            }
        }
        return OTHER;
    }
}
