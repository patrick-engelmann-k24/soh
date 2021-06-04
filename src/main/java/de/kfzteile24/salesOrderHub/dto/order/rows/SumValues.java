package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SumValues {
    private BigDecimal salesValueGross;
    private BigDecimal salesValueNet;
    private BigDecimal goodsValueGross;
    private BigDecimal goodsValueNet;
    private BigDecimal depositGross;
    private BigDecimal depositNet;
    private BigDecimal bulkyGoodsGross;
    private BigDecimal bulkyGoodsNet;
    private BigDecimal riskyGoodsGross;
    private BigDecimal riskyGoodsNet;
    private BigDecimal discountGross;
    private BigDecimal discountNet;
    private BigDecimal totalDiscountedNet;
    private BigDecimal exchangePartValueGross;
    private BigDecimal exchangePartValueNet;
    private BigDecimal rrpGross;
    private BigDecimal rrpNet;
    private BigDecimal undiscountedSalesValueGross;
    private BigDecimal undiscountedSalesValueNet;
}
