package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnitValues {
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
    private BigDecimal rrpNet;
    private BigDecimal rrpGross;
    private BigDecimal discountGross;
    private BigDecimal discountNet;
    private BigDecimal discountedGross;
    private BigDecimal discountedNet;
    private BigDecimal undiscountedSalesValueGross;
    private BigDecimal undiscountedSalesValueNet;
    private BigDecimal exchangePartValueGross;
    private BigDecimal exchangePartValueNet;
}
