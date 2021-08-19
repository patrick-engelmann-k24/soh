package de.kfzteile24.salesOrderHub.converter;

import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.LogisticalUnits;
import de.kfzteile24.salesOrderHub.dto.order.Rows;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.PartIdentificationProperties;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.UnitValues;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Component
public class OrderRowConverter implements Converter<OrderJSON, List<OrderRows>> {

    @Override
    public List<OrderRows> convert(OrderJSON orderJSON) {
        return orderJSON.getOrderRows().stream()
                .map(row ->convertRow(row, orderJSON))
                .collect(toList());
    }

    private OrderRows convertRow(Rows rows, OrderJSON orderJSON) {
        final var logisticalUnit = findLogisticalUnit(rows, orderJSON)
                .orElseThrow(() ->
                        new IllegalStateException("Could not find LogisticalUnit containing the item for rowKey " +
                                rows.getRowKey()));

        return OrderRows.builder()
                .rowKey(parseInt(rows.getRowKey()))
                .isCancelled(false)
                .isPriceHammer(rows.getItemInformation().getPricehammer())
                .sku(rows.getSku())
                .name(rows.getItemInformation().getOrder().getName())
                .ean(firstEntryOrNull(rows.getItemNumbers().getEan()))
                .manufacturerProductNumber(rows.getItemNumbers().getManufacturerProductNumber())
                .dataSupplierNumber(rows.getItemNumbers().getDataSupplierNumber())
                .genart(null)
                .setReferenceId(null)
                .setReferenceName(null)
                .customerNote(null)
                .quantity(rows.getQuantity())
                .partIdentificationProperties(convertPartIdentificationProperties(rows))
                .estimatedDeliveryDate(null)
                .clickCollectBranchId(null)
                .shippingType(logisticalUnit.getShippingType())
                .shippingAddressKey(parseInt(logisticalUnit.getShippingAddressKey()))
                .shippingProvider(logisticalUnit.getShippingProvider())
                .trackingNumbers(logisticalUnit.getTrackingNumber() != null ?
                        List.of(logisticalUnit.getTrackingNumber()) : emptyList())
                .unitValues(convertUnitValues(rows))
                .sumValues(convertSumValues(rows))
                .build();
    }

    private String firstEntryOrNull(List<String> entries) {
        return entries == null ? null :
                entries.stream().findFirst().orElse(null);
    }

    private PartIdentificationProperties convertPartIdentificationProperties(Rows rows) {
        final var partIdentificationProperties = rows.getPartIdentificationProperties();

        return PartIdentificationProperties.builder()
                .carTypeNumber(partIdentificationProperties.getCarTypeNumber())
                .carSelectionType(partIdentificationProperties.getCarSelectionType())
                .cdhVehicleId(null)
                .hsn(partIdentificationProperties.getHsn())
                .tsn(partIdentificationProperties.getTsn())
                .build();
    }

    private Optional<LogisticalUnits> findLogisticalUnit(Rows rows, OrderJSON source) {
        return source.getLogisticalUnits().stream()
                .filter(unit -> unit.getLogisticalItems().stream()
                        .anyMatch(item -> rows.getRowKey().equals(item.getRowKey())))
                .findAny();
    }

    private UnitValues convertUnitValues(Rows rows) {
        final var unitValues = rows.getUnitValues();

        return UnitValues.builder()
                .rrpGross(unitValues.getRrpGross())
                .rrpNet(unitValues.getRrpNet())
                .goodsValueGross(unitValues.getGoodsValueGross())
                .goodsValueNet(unitValues.getGoodsValueNet())
                .depositGross(unitValues.getDepositGross())
                .depositNet(unitValues.getDepositNet())
                .bulkyGoodsGross(unitValues.getBulkyGoodsGross())
                .bulkyGoodsNet(unitValues.getBulkyGoodsNet())
                .riskyGoodsGross(unitValues.getRiskyGoodsGross())
                .riskyGoodsNet(unitValues.getRiskyGoodsNet())
                .discountGross(unitValues.getDiscountGross())
                .discountNet(unitValues.getDiscountNet())
                .discountedGross(unitValues.getDiscountedGross())
                .discountedNet(unitValues.getDiscountedNet())
                .exchangePartValueGross(unitValues.getExchangePartValueGross())
                .exchangePartValueNet(unitValues.getExchangePartValueNet())
                .build();
    }

    private SumValues convertSumValues(Rows rows) {
        final var sumValues = rows.getSumValues();

        return SumValues.builder()
                .goodsValueGross(sumValues.getGoodsValueGross())
                .goodsValueNet(sumValues.getGoodsValueNet())
                .depositGross(sumValues.getDepositGross())
                .depositNet(sumValues.getDepositNet())
                .bulkyGoodsGross(sumValues.getBulkyGoodsGross())
                .bulkyGoodsNet(sumValues.getBulkyGoodsNet())
                .riskyGoodsGross(sumValues.getRiskyGoodsGross())
                .riskyGoodsNet(sumValues.getRiskyGoodsNet())
                .discountGross(sumValues.getDiscountGross())
                .discountNet(sumValues.getDiscountNet())
                .totalDiscountedGross(sumValues.getTotalDiscountedGross())
                .totalDiscountedNet(sumValues.getTotalDiscountedNet())
                .exchangePartValueGross(sumValues.getExchangePartValueGross())
                .exchangePartValueNet(sumValues.getExchangePartValueNet())
                .build();
    }
}
