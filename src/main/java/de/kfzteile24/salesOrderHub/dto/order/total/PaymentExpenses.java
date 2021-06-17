package de.kfzteile24.salesOrderHub.dto.order.total;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentExpenses {
    private BigDecimal cashOnDelivery;
    private BigDecimal creditCardPayment;
}
