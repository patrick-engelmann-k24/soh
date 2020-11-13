package de.kfzteile24.salesOrderHub.dto.order.header;

import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.dto.order.customer.Customer;
import de.kfzteile24.salesOrderHub.dto.order.riskcheck.RiskCheck;
import de.kfzteile24.salesOrderHub.dto.order.total.PaymentExpenses;
import de.kfzteile24.salesOrderHub.dto.order.total.Shipping;
import de.kfzteile24.salesOrderHub.dto.order.total.Taxes;
import lombok.Data;

@Data
public class Total {
    Number goodsTotalGross;
    Number goodsTotalNet;
    String salesValueTotalGross;
    String salesValueTotalNet;
    Number subtotalGross;
    Number subtotalNet;
    Shipping shippingTotalsGross;
    Shipping shippingTotalsNet;
    PaymentExpenses paymentExpensesGross;
    PaymentExpenses paymentExpensesNet;
    String grandtotalGross;
    String grandtotalNet;
    Taxes grandtotalTaxes;
    Number totalDiscountGross;
    Number totalDiscountNet;
    Number paymentTotal;
    RiskCheck riskCheck;
    Customer customer;
}
