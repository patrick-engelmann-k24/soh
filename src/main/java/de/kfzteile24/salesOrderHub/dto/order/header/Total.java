package de.kfzteile24.salesOrderHub.dto.order.header;

import de.kfzteile24.salesOrderHub.dto.order.customer.Customer;
import de.kfzteile24.salesOrderHub.dto.order.riskcheck.RiskCheck;
import de.kfzteile24.salesOrderHub.dto.order.total.PaymentExpenses;
import de.kfzteile24.salesOrderHub.dto.order.total.Shipping;
import de.kfzteile24.salesOrderHub.dto.order.total.Taxes;
import lombok.Data;

@Data
public class Total {
    private Number goodsTotalGross;
    private Number goodsTotalNet;
    private String salesValueTotalGross;
    private String salesValueTotalNet;
    private Number subtotalGross;
    private Number subtotalNet;
    private Shipping shippingTotalsGross;
    private Shipping shippingTotalsNet;
    private PaymentExpenses paymentExpensesGross;
    private PaymentExpenses paymentExpensesNet;
    private String grandtotalGross;
    private String grandtotalNet;
    private Taxes grandtotalTaxes;
    private Number totalDiscountGross;
    private Number totalDiscountNet;
    private Number paymentTotal;
    private RiskCheck riskCheck;
    private Customer customer;
}
