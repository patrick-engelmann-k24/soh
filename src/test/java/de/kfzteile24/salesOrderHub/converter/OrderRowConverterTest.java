package de.kfzteile24.salesOrderHub.converter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readOrderJson;
import static org.assertj.core.api.Assertions.assertThat;

class OrderRowConverterTest {
    private final OrderRowConverter orderRowConverter = new OrderRowConverter();

    @Test
    public void orderRowsAreConvertedCorrectly() {
        var orderJSON = readOrderJson("examples/completeOrder.json");
        orderJSON.getOrderHeader().getCustomer().setContactId(UUID.randomUUID().toString());

        final var convertedOrderRows = orderRowConverter.convert(orderJSON);
        final var originalOrderRows = orderJSON.getOrderRows();

        assertThat(convertedOrderRows).isNotNull();
        assertThat(convertedOrderRows.size()).isEqualTo(originalOrderRows.size());
        for (int i = 0; i < originalOrderRows.size(); i++) {
            final var originalOrderRow = originalOrderRows.get(i);
            final var convertedOrderRow = convertedOrderRows.get(i);

            assertThat(convertedOrderRow.getRowKey()).isEqualTo(Integer.parseInt(originalOrderRow.getRowKey()));
            assertThat(convertedOrderRow.getIsCancelled()).isFalse();
            assertThat(convertedOrderRow.getSku()).isEqualTo(originalOrderRow.getSku());
            assertThat(convertedOrderRow.getName())
                    .isEqualTo(originalOrderRow.getItemInformation().getOrder().getName());
            assertThat(convertedOrderRow.getEan()).isEqualTo(originalOrderRow.getItemNumbers().getEan().get(0));
            assertThat(convertedOrderRow.getGenart()).isNull();
            assertThat(convertedOrderRow.getSetReferenceId()).isNull();
            assertThat(convertedOrderRow.getSetReferenceName()).isNull();
            assertThat(convertedOrderRow.getCustomerNote()).isNull();
            assertThat(convertedOrderRow.getQuantity()).isEqualTo(originalOrderRow.getQuantity());

            final var convertedPart = convertedOrderRow.getPartIdentificationProperties();
            final var originalPart = originalOrderRow.getPartIdentificationProperties();
            assertThat(convertedPart.getCarTypeNumber()).isEqualTo(originalPart.getCarTypeNumber());
            assertThat(convertedPart.getCarSelectionType()).isEqualTo(originalPart.getCarSelectionType());
            assertThat(convertedPart.getCdhVehicleId()).isNull();
            assertThat(convertedPart.getHsn()).isEqualTo(originalPart.getHsn());
            assertThat(convertedPart.getTsn()).isEqualTo(originalPart.getTsn());

            assertThat(convertedOrderRow.getEstimatedDeliveryDate()).isNull();

            final var logisticalUnit = orderJSON.getLogisticalUnits().get(0);
            assertThat(convertedOrderRow.getShippingType()).isEqualTo(logisticalUnit.getShippingType());
            assertThat(convertedOrderRow.getShippingAddressKey())
                    .isEqualTo(Integer.parseInt(logisticalUnit.getShippingAddressKey()));
            assertThat(convertedOrderRow.getShippingProvider()).isEqualTo(logisticalUnit.getShippingProvider());
            assertThat(convertedOrderRow.getTrackingNumbers().size()).isEqualTo(0);
            assertThat(convertedOrderRow.getClickCollectBranchId()).isNull();

            final var convertedUnitValues = convertedOrderRow.getUnitValues();
            final var originalUnitValues = originalOrderRow.getUnitValues();
            assertThat(convertedUnitValues.getRrpGross()).isEqualTo(originalUnitValues.getRrpGross());
            assertThat(convertedUnitValues.getRrpNet()).isEqualTo(originalUnitValues.getRrpNet());
            assertThat(convertedUnitValues.getGoodsValueGross()).isEqualTo(originalUnitValues.getGoodsValueGross());
            assertThat(convertedUnitValues.getGoodsValueNet()).isEqualTo(originalUnitValues.getGoodsValueNet());
            assertThat(convertedUnitValues.getDepositGross()).isEqualTo(originalUnitValues.getDepositGross());
            assertThat(convertedUnitValues.getDepositNet()).isEqualTo(originalUnitValues.getDepositNet());
            assertThat(convertedUnitValues.getBulkyGoodsGross()).isEqualTo(originalUnitValues.getBulkyGoodsGross());
            assertThat(convertedUnitValues.getBulkyGoodsNet()).isEqualTo(originalUnitValues.getBulkyGoodsNet());
            assertThat(convertedUnitValues.getRiskyGoodsGross()).isEqualTo(originalUnitValues.getRiskyGoodsGross());
            assertThat(convertedUnitValues.getRiskyGoodsNet()).isEqualTo(originalUnitValues.getRiskyGoodsNet());
            assertThat(convertedUnitValues.getDiscountGross()).isEqualTo(originalUnitValues.getDiscountGross());
            assertThat(convertedUnitValues.getDiscountNet()).isEqualTo(originalUnitValues.getDiscountNet());
            assertThat(convertedUnitValues.getDiscountedGross()).isEqualTo(originalUnitValues.getDiscountedGross());
            assertThat(convertedUnitValues.getDiscountedNet()).isEqualTo(originalUnitValues.getDiscountedNet());
            assertThat(convertedUnitValues.getExchangePartValueGross())
                    .isEqualTo(originalUnitValues.getExchangePartValueGross());
            assertThat(convertedUnitValues.getExchangePartValueNet())
                    .isEqualTo(originalUnitValues.getExchangePartValueNet());

            final var convertedSumValues = convertedOrderRow.getSumValues();
            final var originalSumValues = originalOrderRow.getSumValues();
            assertThat(convertedSumValues.getGoodsValueGross()).isEqualTo(originalSumValues.getGoodsValueGross());
            assertThat(convertedSumValues.getGoodsValueNet()).isEqualTo(originalSumValues.getGoodsValueNet());
            assertThat(convertedSumValues.getDepositGross()).isEqualTo(originalSumValues.getDepositGross());
            assertThat(convertedSumValues.getDepositNet()).isEqualTo(originalSumValues.getDepositNet());
            assertThat(convertedSumValues.getBulkyGoodsGross()).isEqualTo(originalSumValues.getBulkyGoodsGross());
            assertThat(convertedSumValues.getBulkyGoodsNet()).isEqualTo(originalSumValues.getBulkyGoodsNet());
            assertThat(convertedSumValues.getRiskyGoodsGross()).isEqualTo(originalSumValues.getRiskyGoodsGross());
            assertThat(convertedSumValues.getRiskyGoodsNet()).isEqualTo(originalSumValues.getRiskyGoodsNet());
            assertThat(convertedSumValues.getDiscountGross()).isEqualTo(originalSumValues.getDiscountGross());
            assertThat(convertedSumValues.getDiscountNet()).isEqualTo(originalSumValues.getDiscountNet());
            assertThat(convertedSumValues.getTotalDiscountedGross())
                    .isEqualTo(originalSumValues.getTotalDiscountedGross());
            assertThat(convertedSumValues.getTotalDiscountedNet()).isEqualTo(originalSumValues.getTotalDiscountedNet());
            assertThat(convertedSumValues.getExchangePartValueGross())
                    .isEqualTo(originalSumValues.getExchangePartValueGross());
            assertThat(convertedSumValues.getExchangePartValueNet())
                    .isEqualTo(originalSumValues.getExchangePartValueNet());
        }
    }

}