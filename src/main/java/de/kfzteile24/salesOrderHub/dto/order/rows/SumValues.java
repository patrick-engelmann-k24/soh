package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

@Data
public class SumValues {
    private Number salesValueGross;
    private Number salesValueNet;
    private Number goodsValueGross;
    private Number goodsValueNet;
    private Number depositGross;
    private Number depositNet;
    private Number bulkyGoodsGross;
    private Number bulkyGoodsNet;
    private Number riskyGoodsGross;
    private Number riskyGoodsNet;
    private Number discountGross;
    private Number discountNet;
    private Number totalDiscountedNet;
    private Number exchangePartValueGross;
    private Number exchangePartValueNet;
    private Number rrpGross;
    private Number rrpNet;
    private Number undiscountedSalesValueGross;
    private Number undiscountedSalesValueNet;
}
