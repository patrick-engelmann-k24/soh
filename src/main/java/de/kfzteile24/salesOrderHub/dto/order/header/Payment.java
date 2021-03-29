package de.kfzteile24.salesOrderHub.dto.order.header;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class Payment {
    private String type;
    private BigDecimal value;
    private String paymentTransactionId;
    private PaymentProviderData paymentProviderData;
    private String category;

}
