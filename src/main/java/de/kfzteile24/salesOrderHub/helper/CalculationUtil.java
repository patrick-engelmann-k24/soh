package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.soh.order.dto.SumValues;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author samet
 */

public class CalculationUtil {
    public static final MathContext MATH_CONTEXT = new MathContext(34, RoundingMode.HALF_UP);
    public static final int PRECISION = 2;  // Precision is taken 2 as in the calculation-service repo

    private CalculationUtil() {}

    public static boolean isNotNullAndNotEqual(BigDecimal targetValue, BigDecimal comparedValue) {
        return targetValue != null &&
                targetValue.compareTo(Optional.ofNullable(comparedValue).orElse(BigDecimal.ZERO)) != 0;
    }

    public static BigDecimal round(BigDecimal value, RoundingMode roundingMode) {
        if (value != null && value.scale() > PRECISION) {
            return value.setScale(PRECISION, roundingMode);
        }
        return value;
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

        return netValue.multiply(BigDecimal.valueOf(100).add(taxRate)).divide(BigDecimal.valueOf(100), MATH_CONTEXT);
    }

    public static BigDecimal getNetValue(BigDecimal grossValue, BigDecimal taxRate) {
        if (grossValue == null || taxRate == null)
            return grossValue;

        return grossValue.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(100).add(taxRate), MATH_CONTEXT);
    }

    public static BigDecimal getDiscountResult(BigDecimal goodValue, BigDecimal discountedValue) {
        if (goodValue == null)
            return null;

        return goodValue.subtract(Optional.ofNullable(discountedValue).orElse(BigDecimal.ZERO));
    }

    public static BigDecimal getSumOfGoodsNetFromSumOfGoodsGross(BigDecimal salesPriceGross, BigDecimal quantity, BigDecimal taxRate) {
        if (salesPriceGross == null || quantity == null || taxRate == null) {
            return salesPriceGross;
        }
        var goodsValueGross = salesPriceGross.divide(quantity, MATH_CONTEXT);
        var goodsValueNet = getNetValue(goodsValueGross, taxRate);
        return quantity.multiply(goodsValueNet);
    }
}
