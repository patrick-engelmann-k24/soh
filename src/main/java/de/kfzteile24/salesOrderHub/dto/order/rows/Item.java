package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

@Data
public class Item {
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
    Number exchangePartValueGross;
    Number exchangePartValueNet;
    Number discountGross;
    Number discountNet;
}
