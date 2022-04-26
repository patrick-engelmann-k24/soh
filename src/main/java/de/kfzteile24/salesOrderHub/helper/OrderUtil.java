package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
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

import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getGrossValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getMultipliedValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getValueOrDefault;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.isGreater;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.isNotNullAndNotEqual;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.round;
import static java.math.RoundingMode.HALF_DOWN;
import static java.math.RoundingMode.HALF_UP;

/**
 * @author samet
 */


@Component
@Slf4j
@RequiredArgsConstructor
public class OrderUtil {

    private final ObjectUtil objectUtil;

    public Order copyOrderJson(Order orderJson) {
        return objectUtil.deepCopyOf(orderJson, Order.class);
    }

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

    public OrderRows createOrderFromOriginalSalesOrder(SalesOrder salesOrder, CoreSalesFinancialDocumentLine item, Integer lastRowKey) {
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

    public OrderRows recalculateOrderRow(OrderRows orderRow, CreditNoteLine item) {
        decreaseOrderRowByNewItemValues(orderRow, item);
        return orderRow;
    }

    private OrderRows filterFromLatestJson(SalesOrder salesOrder, CoreSalesFinancialDocumentLine item) {
        return filterFromOrder(item, salesOrder.getLatestJson());
    }

    private OrderRows filterFromOriginalOrder(SalesOrder salesOrder, CoreSalesFinancialDocumentLine item) {
        return filterFromOrder(item, (Order) salesOrder.getOriginalOrder());
    }

    private OrderRows createNewOrderRow(CoreSalesFinancialDocumentLine item, String shippingType, Integer lastRowKey) {
        var unitPriceGross = getGrossValue(item.getUnitNetAmount(), item.getTaxRate());
        var unitPriceNet = item.getUnitNetAmount();
        var sumOfGoodsPriceGross = getMultipliedValue(unitPriceGross, item.getQuantity());
        var sumOfGoodsPriceNet = getMultipliedValue(unitPriceNet, item.getQuantity());

        return OrderRows.builder()
                .rowKey(lastRowKey + 1)
                .shippingType(shippingType)
                .sku(item.getItemNumber())
                .quantity(Optional.ofNullable(item.getQuantity()).orElse(BigDecimal.ONE))
                .taxRate(Optional.ofNullable(item.getTaxRate()).orElse(BigDecimal.ZERO))
                .unitValues(roundUnitValues(UnitValues.builder()
                        .goodsValueGross(unitPriceGross)
                        .goodsValueNet(unitPriceNet)
                        .discountedGross(unitPriceGross)
                        .discountedNet(unitPriceNet)
                        .build()))
                .sumValues(roundSumValues(SumValues.builder()
                        .goodsValueGross(sumOfGoodsPriceGross)
                        .goodsValueNet(sumOfGoodsPriceNet)
                        .totalDiscountedGross(sumOfGoodsPriceGross)
                        .totalDiscountedNet(sumOfGoodsPriceNet)
                        .build()))
                .build();
    }

    private OrderRows filterFromOrder(CoreSalesFinancialDocumentLine item, Order latestOrder) {
        var orderRow = OrderMapper.INSTANCE.toOrderRow(latestOrder.getOrderRows().stream()
                .filter(row -> row.getSku().equals(item.getItemNumber()))
                .findFirst().orElse(null));
        if(orderRow == null){
            log.info("Order row with sku: {} was not found for order with order number: {}",
                    item.getItemNumber(), latestOrder.getOrderHeader().getOrderNumber());
        }
        updateOrderRowByNewItemValues(orderRow, item);
        return orderRow;
    }

    protected void updateOrderRowByNewItemValues(OrderRows row, CoreSalesFinancialDocumentLine item) {
        if (row != null) {
            if (isNotNullAndNotEqual(item.getTaxRate(), row.getTaxRate())) {
                updateOrderRowByTaxRate(row, item.getTaxRate());
            }

            if (isNotNullAndNotEqual(item.getUnitNetAmount(), row.getUnitValues().getGoodsValueNet())) {
                updateOrderRowByUnitPriceNet(row, item.getUnitNetAmount());
            }

            if (isNotNullAndNotEqual(item.getQuantity(), row.getQuantity())) {
                updateOrderRowByQuantity(row, item.getQuantity());
            }
        }
    }

    protected void decreaseOrderRowByNewItemValues(OrderRows row, CreditNoteLine item) {
        if (row != null) {
            if (isNotNullAndNotEqual(item.getTaxRate(), row.getTaxRate())) {
                updateOrderRowByTaxRate(row, item.getTaxRate());
            }
            decreaseOrderRowByUnitNetValue(row, item);
            decreaseOrderRowByQuantity(row, item);
        }
    }

    private void decreaseOrderRowByUnitNetValue(OrderRows row, CreditNoteLine item) {
        var rowUnitNetValue = getValueOrDefault(row.getUnitValues().getGoodsValueNet(), BigDecimal.ZERO);
        var itemUnitNetValue = getValueOrDefault(item.getUnitNetAmount(), BigDecimal.ZERO);

        if (!itemUnitNetValue.equals(BigDecimal.ZERO)) {
            var updatedValue = rowUnitNetValue.subtract(itemUnitNetValue);
            updateOrderRowByUnitPriceNet(row, updatedValue);
        }
    }

    protected void decreaseOrderRowByQuantity(OrderRows row, CreditNoteLine item) {
        var rowQuantity = getValueOrDefault(row.getQuantity(), BigDecimal.ZERO);
        var itemQuantity = getValueOrDefault(item.getQuantity(), BigDecimal.ZERO);

        if (isGreater(itemQuantity, rowQuantity)) {
            throw new IllegalArgumentException("Return item quantity must be less than or equal row quantity");
        }

        var updatedQuantity = rowQuantity.subtract(itemQuantity);
        updateOrderRowByQuantity(row, updatedQuantity);
    }

    private void updateOrderRowByTaxRate(OrderRows row, BigDecimal taxRate) {
        row.setTaxRate(taxRate);
        row.setUnitValues(roundUnitValues(OrderMapper.INSTANCE.updateByTaxRate(row.getUnitValues(), taxRate)));
        row.setSumValues(roundSumValues(OrderMapper.INSTANCE.toSumValues(row.getUnitValues(), row.getQuantity())));
    }

    private void updateOrderRowByQuantity(OrderRows row, BigDecimal quantity) {
        row.setQuantity(quantity);
        row.setSumValues(roundSumValues(OrderMapper.INSTANCE.toSumValues(row.getUnitValues(), quantity)));
    }

    private void updateOrderRowByUnitPriceNet(OrderRows row, BigDecimal unitPriceNet) {
        var unitPriceGross = getGrossValue(unitPriceNet, row.getTaxRate());
        row.setUnitValues(roundUnitValues(OrderMapper.INSTANCE.updateByGoodsValue(row.getUnitValues(), unitPriceGross, unitPriceNet)));
        row.setSumValues(roundSumValues(OrderMapper.INSTANCE.toSumValues(row.getUnitValues(), row.getQuantity())));
    }
  
    private UnitValues roundUnitValues(UnitValues unitValues) {
        //Rounding Modes are given as in the calculation-service repo
        unitValues.setGoodsValueGross(round(unitValues.getGoodsValueGross(), HALF_UP));
        unitValues.setGoodsValueNet(round(unitValues.getGoodsValueNet(), HALF_UP));
        unitValues.setDiscountGross(round(unitValues.getDiscountGross(), HALF_UP));
        unitValues.setDiscountNet(round(unitValues.getDiscountNet(), HALF_UP));
        unitValues.setDiscountedGross(round(unitValues.getDiscountedGross(), HALF_UP));
        unitValues.setDiscountedNet(round(unitValues.getDiscountedNet(), HALF_UP));
        unitValues.setRrpGross(round(unitValues.getRrpGross(), HALF_UP));
        unitValues.setRrpNet(round(unitValues.getRrpNet(), HALF_UP));
        unitValues.setDepositGross(round(unitValues.getDepositGross(), HALF_UP));
        unitValues.setDepositNet(round(unitValues.getDepositNet(), HALF_UP));
        unitValues.setBulkyGoodsGross(round(unitValues.getBulkyGoodsGross(), HALF_UP));
        unitValues.setBulkyGoodsNet(round(unitValues.getBulkyGoodsNet(), HALF_UP));
        unitValues.setRiskyGoodsGross(round(unitValues.getRiskyGoodsGross(), HALF_UP));
        unitValues.setRiskyGoodsNet(round(unitValues.getRiskyGoodsNet(), HALF_UP));
        unitValues.setExchangePartValueGross(round(unitValues.getExchangePartValueGross(), HALF_UP));
        unitValues.setExchangePartValueNet(round(unitValues.getExchangePartValueNet(), HALF_UP));
        return unitValues;
    }

    private SumValues roundSumValues(SumValues sumValues) {
        //Rounding Modes are given as in the calculation-service repo
        sumValues.setGoodsValueGross(round(sumValues.getGoodsValueGross(), HALF_UP));
        sumValues.setGoodsValueNet(round(sumValues.getGoodsValueNet(), HALF_UP));
        sumValues.setDiscountGross(round(sumValues.getDiscountGross(), HALF_UP));
        sumValues.setDiscountNet(round(sumValues.getDiscountNet(), HALF_UP));
        sumValues.setTotalDiscountedGross(round(sumValues.getTotalDiscountedGross(), HALF_DOWN));
        sumValues.setTotalDiscountedNet(round(sumValues.getTotalDiscountedNet(), HALF_DOWN));
        sumValues.setDepositGross(round(sumValues.getDepositGross(), HALF_UP));
        sumValues.setDepositNet(round(sumValues.getDepositNet(), HALF_UP));
        sumValues.setBulkyGoodsGross(round(sumValues.getBulkyGoodsGross(), HALF_UP));
        sumValues.setBulkyGoodsNet(round(sumValues.getBulkyGoodsNet(), HALF_UP));
        sumValues.setRiskyGoodsGross(round(sumValues.getRiskyGoodsGross(), HALF_UP));
        sumValues.setRiskyGoodsNet(round(sumValues.getRiskyGoodsNet(), HALF_UP));
        sumValues.setExchangePartValueGross(round(sumValues.getExchangePartValueGross(), HALF_UP));
        sumValues.setExchangePartValueNet(round(sumValues.getExchangePartValueNet(), HALF_UP));
        return sumValues;
    }
}
