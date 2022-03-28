package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.subsequent.SubsequentDeliveryItem;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.UnitValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.*;

/**
 * @author samet
 */


@Component
@Slf4j
@RequiredArgsConstructor
public class OrderUtil {

    public Integer getLastRowKey(SalesOrder originalSalesOrder) {
        var originalOrder = (Order) originalSalesOrder.getOriginalOrder();
        return originalOrder.getOrderRows().stream().map(OrderRows::getRowKey).reduce(0, Integer::max);
    }

    public SalesOrder removeCancelledOrderRowsFromLatestJson(SalesOrder salesOrder) {
        salesOrder.getLatestJson().setOrderRows(
                salesOrder.getLatestJson().getOrderRows().stream()
                        .filter(row -> !row.getIsCancelled())
                        .collect(Collectors.toList()));
        return salesOrder;
    }

    public OrderRows createOrderFromOriginalSalesOrder(SalesOrder salesOrder, SubsequentDeliveryItem item, Integer lastRowKey) {
        var orderRow = filterFromLatestJson(salesOrder, item);
        if (orderRow == null) {
            orderRow = filterFromOriginalOrder(salesOrder, item);
        }
        if (orderRow == null) {
            var shippingType = ((Order) salesOrder.getOriginalOrder()).getOrderRows().get(0).getShippingType();
            orderRow = createNewOrderRow(item, shippingType, lastRowKey);
        }
        orderRow.setIsCancelled(false);
        return orderRow;
    }

    private OrderRows filterFromLatestJson(SalesOrder salesOrder, SubsequentDeliveryItem item) {
        return filterFromOrder(item, salesOrder.getLatestJson());
    }

    private OrderRows filterFromOriginalOrder(SalesOrder salesOrder, SubsequentDeliveryItem item) {
        return filterFromOrder(item, (Order) salesOrder.getOriginalOrder());
    }

    private OrderRows createNewOrderRow(SubsequentDeliveryItem item, String shippingType, Integer lastRowKey) {
        var unitPriceGross = item.getUnitPriceGross();
        var unitPriceNet = getNetValue(item.getUnitPriceGross(), item.getTaxRate());
        var sumOfGoodsPriceGross =
                Optional.ofNullable(item.getSalesPriceGross())
                        .orElse(getMultipliedValue(item.getUnitPriceGross(), item.getQuantity()));
        var sumOfGoodsPriceNet = getSumOfGoodsNetFromSumOfGoodsGross(
                item.getSalesPriceGross(),
                item.getQuantity(),
                item.getTaxRate());

        return OrderRows.builder()
                .rowKey(lastRowKey + 1)
                .shippingType(shippingType)
                .sku(item.getSku())
                .quantity(Optional.ofNullable(item.getQuantity()).orElse(BigDecimal.ONE))
                .taxRate(Optional.ofNullable(item.getTaxRate()).orElse(BigDecimal.ZERO))
                .unitValues(UnitValues.builder()
                        .goodsValueGross(unitPriceGross)
                        .goodsValueNet(unitPriceNet)
                        .discountedGross(unitPriceGross)
                        .discountedNet(unitPriceNet)
                        .build())
                .sumValues(SumValues.builder()
                        .goodsValueGross(sumOfGoodsPriceGross)
                        .goodsValueNet(sumOfGoodsPriceNet)
                        .totalDiscountedGross(sumOfGoodsPriceGross)
                        .totalDiscountedNet(sumOfGoodsPriceNet)
                        .build())
                .build();
    }

    private OrderRows filterFromOrder(SubsequentDeliveryItem item, Order latestOrder) {
        var orderRow = latestOrder.getOrderRows().stream()
                .filter(row -> row.getSku().equals(item.getSku()))
                .findFirst().orElse(null);
        updateOrderRowByNewItemValues(orderRow, item);
        return orderRow;
    }

    protected void updateOrderRowByNewItemValues(OrderRows row, SubsequentDeliveryItem item) {
        if (row != null) {
            if (isNotNullAndNotEqual(item.getTaxRate(), row.getTaxRate())) {
                updateOrderRowByTaxRate(row, item.getTaxRate());
            }

            if (isNotNullAndNotEqual(item.getUnitPriceGross(), row.getUnitValues().getGoodsValueGross())) {
                updateOrderRowByUnitPriceGross(row, item.getUnitPriceGross());
            }

            if (isNotNullAndNotEqual(item.getQuantity(), row.getQuantity())) {
                updateOrderRowByQuantity(row, item.getQuantity());
            }

            if (isNotNullAndNotEqual(item.getSalesPriceGross(), row.getSumValues().getGoodsValueGross())) {
                updateOrderRowBySalesPriceGross(row, item.getSalesPriceGross());
            }
        }
    }

    private void updateOrderRowByTaxRate(OrderRows row, BigDecimal taxRate) {
        row.setTaxRate(taxRate);
        row.setUnitValues(OrderMapper.INSTANCE.updateByTaxRate(row.getUnitValues(), taxRate));
        row.setSumValues(OrderMapper.INSTANCE.toSumValues(row.getUnitValues(), row.getQuantity()));
    }

    private void updateOrderRowByQuantity(OrderRows row, BigDecimal quantity) {
        row.setQuantity(quantity);
        row.setSumValues(OrderMapper.INSTANCE.toSumValues(row.getUnitValues(), quantity));
    }

    private void updateOrderRowByUnitPriceGross(OrderRows row, BigDecimal unitPriceGross) {
        var unitPriceNet = getNetValue(unitPriceGross, row.getTaxRate());
        row.setUnitValues(OrderMapper.INSTANCE.updateByGoodsValue(row.getUnitValues(), unitPriceGross, unitPriceNet));
        row.setSumValues(OrderMapper.INSTANCE.toSumValues(row.getUnitValues(), row.getQuantity()));
    }

    private void updateOrderRowBySalesPriceGross(OrderRows row, BigDecimal salesPriceGross) {
        BigDecimal salesPriceNet = getSumOfGoodsNetFromSumOfGoodsGross(salesPriceGross, row.getQuantity(), row.getTaxRate());
        row.setSumValues(OrderMapper.INSTANCE.updateByGoodsValue(row.getSumValues(), salesPriceGross, salesPriceNet));
    }
}
