package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

@Data
public class SumValues {
    Number salesValueGross;
    Number salesValueNet;
    Number goodsValueGross;
    Number goodsValueNet;
    Number depositGross;
    Number depositNet;
    Number bulkyGoodsGross;
    Number bulkyGoodsNet;
    Number riskyGoodsGross;
    Number riskyGoodsNet;
    Number discountGross;
    Number discountNet;
    Number totalDiscountedNet;
    Number exchangePartValueGross;
    Number exchangePartValueNet;
    Number rrpGross;
    Number rrpNet;
    Number undiscountedSalesValueGross;
    Number undiscountedSalesValueNet;
}
