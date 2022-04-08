package de.kfzteile24.salesOrderHub.services.aggregator;

import de.kfzteile24.salesOrderHub.dto.sns.deliverynote.CoreDeliveryNoteItem;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Totals;
import de.kfzteile24.soh.order.dto.UnitValues;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;

import java.math.BigDecimal;
import java.util.List;

final public class Aggregators {

    public static class SumValuesAggregator implements ArgumentsAggregator {
        @Override
        public SumValues aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
            return SumValues.builder()
                    .goodsValueGross(arguments.get(0, BigDecimal.class))
                    .goodsValueNet(arguments.get(1, BigDecimal.class))
                    .discountNet(arguments.get(2, BigDecimal.class))
                    .discountGross(arguments.get(3, BigDecimal.class))
                    .totalDiscountedGross(arguments.get(4, BigDecimal.class))
                    .totalDiscountedNet(arguments.get(5, BigDecimal.class))
                    .build();
        }
    }

    public static class UpdatedSumValuesAggregator implements ArgumentsAggregator {
        @Override
        public SumValues aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
            return SumValues.builder()
                    .goodsValueGross(arguments.get(12, BigDecimal.class))
                    .goodsValueNet(arguments.get(13, BigDecimal.class))
                    .discountNet(arguments.get(14, BigDecimal.class))
                    .discountGross(arguments.get(15, BigDecimal.class))
                    .totalDiscountedGross(arguments.get(16, BigDecimal.class))
                    .totalDiscountedNet(arguments.get(17, BigDecimal.class))
                    .build();
        }
    }

    public static class UnitValuesAggregator implements ArgumentsAggregator {
        @Override
        public UnitValues aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
            return UnitValues.builder()
                    .goodsValueGross(arguments.get(6, BigDecimal.class))
                    .goodsValueNet(arguments.get(7, BigDecimal.class))
                    .discountGross(arguments.get(8, BigDecimal.class))
                    .discountNet(arguments.get(9, BigDecimal.class))
                    .discountedGross(arguments.get(10, BigDecimal.class))
                    .discountedNet(arguments.get(11, BigDecimal.class))
                    .build();
        }
    }

    public static class CoreDeliveryNoteItemAggregator implements ArgumentsAggregator {
        @Override
        public CoreDeliveryNoteItem aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
            return CoreDeliveryNoteItem.builder()
                    .sku("sku-1")
                    .unitPriceGross(arguments.get(18, BigDecimal.class))
                    .salesPriceGross(arguments.get(19, BigDecimal.class))
                    .quantity(arguments.get(20, BigDecimal.class))
                    .taxRate(BigDecimal.valueOf(10))
                    .build();
        }
    }

    public static class AnotherCoreDeliveryNoteItemAggregator implements ArgumentsAggregator {
        @Override
        public CoreDeliveryNoteItem aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
            return CoreDeliveryNoteItem.builder()
                    .sku("sku-3")
                    .unitPriceGross(arguments.get(18, BigDecimal.class))
                    .salesPriceGross(arguments.get(19, BigDecimal.class))
                    .quantity(arguments.get(20, BigDecimal.class))
                    .taxRate(BigDecimal.valueOf(10))
                    .build();
        }
    }

    public static class TotalsAggregator implements ArgumentsAggregator {
        @Override
        public Totals aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
            return Totals.builder()
                    .goodsTotalGross(arguments.get(21, BigDecimal.class))
                    .goodsTotalNet(arguments.get(22, BigDecimal.class))
                    .grandTotalGross(arguments.get(23, BigDecimal.class))
                    .grandTotalNet(arguments.get(24, BigDecimal.class))
                    .totalDiscountGross(arguments.get(25, BigDecimal.class))
                    .totalDiscountNet(arguments.get(26, BigDecimal.class))
                    .paymentTotal(arguments.get(27, BigDecimal.class))
                    .grandTotalTaxes(List.of(GrandTotalTaxes.builder()
                            .value(arguments.get(28, BigDecimal.class))
                            .rate(BigDecimal.valueOf(10))
                            .build()))
                    .build();
        }
    }

    public static class UpdatedTotalsAggregator implements ArgumentsAggregator {
        @Override
        public Totals aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
            return Totals.builder()
                    .goodsTotalGross(arguments.get(29, BigDecimal.class))
                    .goodsTotalNet(arguments.get(30, BigDecimal.class))
                    .grandTotalGross(arguments.get(31, BigDecimal.class))
                    .grandTotalNet(arguments.get(32, BigDecimal.class))
                    .totalDiscountGross(arguments.get(33, BigDecimal.class))
                    .totalDiscountNet(arguments.get(34, BigDecimal.class))
                    .paymentTotal(arguments.get(35, BigDecimal.class))
                    .grandTotalTaxes(List.of(GrandTotalTaxes.builder()
                            .value(arguments.get(36, BigDecimal.class))
                            .rate(BigDecimal.valueOf(10))
                            .build()))
                    .build();
        }
    }
}
