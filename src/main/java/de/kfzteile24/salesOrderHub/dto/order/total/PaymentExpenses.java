package de.kfzteile24.salesOrderHub.dto.order.total;

import lombok.Data;

@Data
public class PaymentExpenses {
    private String cashOnDelivery;
    private String creditCardPayment;
}
