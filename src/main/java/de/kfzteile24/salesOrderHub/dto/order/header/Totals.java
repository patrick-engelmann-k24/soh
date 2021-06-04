package de.kfzteile24.salesOrderHub.dto.order.header;

import de.kfzteile24.salesOrderHub.dto.order.riskcheck.RiskCheck;
import de.kfzteile24.salesOrderHub.dto.order.total.Taxes;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Totals {
    private BigDecimal goodsTotalGross;
    private BigDecimal goodsTotalNet;
    private BigDecimal salesValueTotalGross;
    private BigDecimal salesValueTotalNet;
    private BigDecimal subtotalGross;
    private BigDecimal subtotalNet;
    private Object shippingTotalsGross;
    private Object shippingTotalsNet;
    private Object paymentExpensesGross;
    private Object paymentExpensesNet;
    private BigDecimal grandtotalGross;
    private BigDecimal grandtotalNet;
    private Taxes[] grandtotalTaxes;
    private BigDecimal totalDiscountGross;
    private BigDecimal totalDiscountNet;
    private BigDecimal paymentTotal;
    private RiskCheck riskCheck;
}
