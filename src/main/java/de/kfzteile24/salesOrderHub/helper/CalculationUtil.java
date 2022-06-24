package de.kfzteile24.salesOrderHub.helper;

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

    public static boolean isNotNullAndEqual(BigDecimal targetValue, BigDecimal comparedValue) {
        return targetValue != null &&
                targetValue.compareTo(Optional.ofNullable(comparedValue).orElse(BigDecimal.ZERO)) == 0;
    }

    public static BigDecimal round(BigDecimal value, RoundingMode roundingMode) {
        if (value != null && value.scale() > PRECISION) {
            return value.setScale(PRECISION, roundingMode);
        }
        return value;
    }

    public static BigDecimal round(BigDecimal value) {
        if (value != null && value.scale() > PRECISION) {
            return value.setScale(PRECISION, RoundingMode.HALF_UP);
        }
        return value;
    }

    public static <T> BigDecimal getSumValue(Function<T, BigDecimal> function, List<T> sumValues) {

        return sumValues.stream().map(function).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal getMultipliedValue(BigDecimal value, BigDecimal quantity) {

        return Optional.ofNullable(value).orElse(BigDecimal.ZERO).multiply(quantity);
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
}
