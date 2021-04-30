package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

@Data
public class UnitValues {
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
    Number rrpNet;
    Number rrpGross;
    Number discountGross;
    Number discountNet;
    Number discountedGross;
    Number discountedNet;
    Number undiscountedSalesValueGross;
    Number undiscountedSalesValueNet;
    Number exchangePartValueGross;
    Number exchangePartValueNet;
}
