package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

@Data
public class Payment {
    String type;
    Number value;
    String paymentTransactionId;
    PaymentProviderData paymentProviderData;

}
