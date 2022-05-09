package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import de.kfzteile24.salesOrderHub.dto.sns.shared.DocumentLine;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.UnitValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getGrossValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getMultipliedValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getValueOrDefault;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.isNotNullAndNotEqual;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.round;
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
        return originalOrder.getOrderRows().stream().map(OrderRows::getRowKey).filter(Objects::nonNull).reduce(0, Integer::max);
    }

    public OrderRows createOrderRowFromOriginalSalesOrder(SalesOrder originalSalesOrder, CoreSalesFinancialDocumentLine item) {
        var shippingType = ((Order) originalSalesOrder.getOriginalOrder()).getOrderRows().get(0).getShippingType();
        return createNewOrderRow(item, originalSalesOrder, shippingType);
    }

    public void updateOrderRowValues(OrderRows orderRow, DocumentLine item) {

        if (orderRow != null) {
            updateOrderRowByUnitPriceNet(orderRow, getValueOrDefault(item.getUnitNetAmount(), BigDecimal.ZERO));
        }
    }

    private List<OrderRows> createRowFromLatestJson(SalesOrder salesOrder, CoreSalesFinancialDocumentLine item) {
        return salesOrder.getLatestJson().getOrderRows().stream()
                .filter(row -> row.getSku().equals(item.getItemNumber()))
                .map(OrderMapper.INSTANCE::toOrderRow)
                .map(row -> {
                    row.setIsCancelled(false);
                    if (row.getQuantity().compareTo(item.getQuantity()) <= 0) {
                        return updateOrderRowByNewItemValues(row, item);
                    } else {
                        item.setQuantity(item.getQuantity().subtract(row.getQuantity()));
                        return row;
                    }
                })
                .collect(Collectors.toList());
    }

    public OrderRows createNewOrderRow(CoreSalesFinancialDocumentLine item, SalesOrder salesOrder,
                                       String shippingType) {
        return buildNewOrderRow(item, salesOrder, shippingType);
    }


    public OrderRows createNewOrderRowFromCreditNoteItem(CreditNoteLine item, SalesOrder salesOrder) {
        return buildNewOrderRow(item, salesOrder, null);
    }

    private OrderRows buildNewOrderRow(DocumentLine item, SalesOrder salesOrder, String shippingType) {

        Integer lastRowKey = getLastRowKey(salesOrder);

        OrderRows originalOrderRow = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(r -> StringUtils.pathEquals(r.getSku(), item.getItemNumber()))
                .findFirst().orElse(OrderRows.builder().build());

        var unitPriceGross = getGrossValue(item.getUnitNetAmount(), item.getTaxRate());
        var unitPriceNet = item.getUnitNetAmount();
        var sumOfGoodsPriceGross = getMultipliedValue(unitPriceGross, item.getQuantity());
        var sumOfGoodsPriceNet = getMultipliedValue(unitPriceNet, item.getQuantity());

        OrderRows orderRow = OrderRows.builder()
                .rowKey(lastRowKey + 1)
                .isCancelled(originalOrderRow.getIsCancelled())
                .isPriceHammer(originalOrderRow.getIsPriceHammer())
                .sku(item.getItemNumber())
                .name(originalOrderRow.getName())
                .dataSupplierNumber(originalOrderRow.getDataSupplierNumber())
                .manufacturerProductNumber(originalOrderRow.getManufacturerProductNumber())
                .ean(originalOrderRow.getEan())
                .genart(originalOrderRow.getGenart())
                .setReferenceId(originalOrderRow.getSetReferenceId())
                .setReferenceName(originalOrderRow.getSetReferenceName())
                .customerNote(originalOrderRow.getCustomerNote())
                .quantity(Optional.ofNullable(item.getQuantity()).orElse(BigDecimal.ONE))
                .taxRate(Optional.ofNullable(item.getTaxRate()).orElse(BigDecimal.ZERO))
                .partIdentificationProperties(originalOrderRow.getPartIdentificationProperties())
                .estimatedDeliveryDate(originalOrderRow.getEstimatedDeliveryDate())
                .shippingType(shippingType != null ? shippingType : originalOrderRow.getShippingType())
                .clickCollectBranchId(originalOrderRow.getClickCollectBranchId())
                .shippingAddressKey(originalOrderRow.getShippingAddressKey())
                .shippingProvider(originalOrderRow.getShippingProvider())
                .trackingNumbers(originalOrderRow.getTrackingNumbers())
                .vendor(originalOrderRow.getVendor())
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
        updateOrderRowValues(orderRow, item);
        return orderRow;
    }

    protected OrderRows updateOrderRowByNewItemValues(OrderRows row, CoreSalesFinancialDocumentLine item) {
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
        return row;
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
        sumValues.setTotalDiscountedGross(round(sumValues.getTotalDiscountedGross(), HALF_UP));
        sumValues.setTotalDiscountedNet(round(sumValues.getTotalDiscountedNet(), HALF_UP));
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
