package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.soh.order.dto.SumValues;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author samet
 */

public class CalculationUtil {

    private CalculationUtil() {}

    public static boolean isNotNullAndNotEqual(BigDecimal targetValue, BigDecimal comparedValue) {
        return targetValue != null &&
                targetValue.compareTo(Optional.ofNullable(comparedValue).orElse(BigDecimal.ZERO)) != 0;
    }

    public static BigDecimal getSumValue(Function<SumValues, BigDecimal> function, List<SumValues> sumValues) {

        return sumValues.stream().map(function).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal getMultipliedValue(BigDecimal value, BigDecimal quantity) {

        return Optional.ofNullable(value).orElse(BigDecimal.ZERO).multiply(quantity);
    }

    public static BigDecimal getGrossValue(BigDecimal netValue, BigDecimal taxRate) {
        if (netValue == null || taxRate == null)
            return netValue;

        return netValue.multiply(BigDecimal.valueOf(100).add(taxRate)).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
    }

    public static BigDecimal getNetValue(BigDecimal grossValue, BigDecimal taxRate) {
        if (grossValue == null || taxRate == null)
            return grossValue;

        return grossValue.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(100).add(taxRate), 2, RoundingMode.DOWN);
    }

    public static BigDecimal getDiscountResult(BigDecimal goodValue, BigDecimal discountValue) {
        if (goodValue == null)
            return null;

        return goodValue.subtract(Optional.ofNullable(discountValue).orElse(BigDecimal.ZERO));
    }

    public static BigDecimal getSumOfGoodsNetFromSumOfGoodsGross(BigDecimal salesPriceGross, BigDecimal quantity, BigDecimal taxRate) {
        if (salesPriceGross == null || quantity == null || taxRate == null) {
            return salesPriceGross;
        }
        var goodsValueGross = salesPriceGross.divide(quantity, 2, RoundingMode.DOWN);
        var goodsValueNet = getNetValue(goodsValueGross, taxRate);
        return quantity.multiply(goodsValueNet);
    }
}
