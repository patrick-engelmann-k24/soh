package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.soh.order.dto.OrderHeader;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.UnitValues;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.DATE_TIME_FORMATTER;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getNetValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getMultipliedValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getDiscountResult;

/**
 * @author samet
 */

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "orderDateTime", source = "orderDateTime", qualifiedByName = "evaluateOrderDateTime")
    OrderHeader toOrderHeader(OrderHeader orderHeader);

    OrderRows toOrderRow(OrderRows orderRow);

    @Mapping(target = "rrpNet", source = "rrpGross", qualifiedByName = "calculateNet")
    @Mapping(target = "goodsValueNet", source = "goodsValueGross", qualifiedByName = "calculateNet")
    @Mapping(target = "depositNet", source = "depositGross", qualifiedByName = "calculateNet")
    @Mapping(target = "bulkyGoodsNet", source = "bulkyGoodsGross", qualifiedByName = "calculateNet")
    @Mapping(target = "riskyGoodsNet", source = "riskyGoodsGross", qualifiedByName = "calculateNet")
    @Mapping(target = "discountGross", source = "discountNet", qualifiedByName = "calculateNet")
    @Mapping(target = "exchangePartValueNet", source = "exchangePartValueGross", qualifiedByName = "calculateNet")
    @Mapping(target = "discountedGross", source = "unitValues", qualifiedByName = "evaluateDiscountGross")
    @Mapping(target = "discountedNet", source = "unitValues", qualifiedByName = "evaluateDiscountNet")
    UnitValues updateByTaxRate(UnitValues unitValues, @Context BigDecimal taxRate);

    @Mapping(target = "goodsValueGross", source = "goodsGrossValue")
    @Mapping(target = "goodsValueNet", source = "goodsNetValue")
    @Mapping(target = "discountGross", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "discountNet", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "discountedGross", source = "goodsGrossValue")
    @Mapping(target = "discountedNet", source = "goodsNetValue")
    UnitValues updateByGoodsValue(UnitValues unitValues, BigDecimal goodsGrossValue, BigDecimal goodsNetValue);

    @Mapping(target = "goodsValueGross", source = "goodsValueGross", qualifiedByName = "multiply")
    @Mapping(target = "goodsValueNet", source = "goodsValueNet", qualifiedByName = "multiply")
    @Mapping(target = "depositGross", source = "depositGross", qualifiedByName = "multiply")
    @Mapping(target = "depositNet", source = "depositNet", qualifiedByName = "multiply")
    @Mapping(target = "bulkyGoodsGross", source = "bulkyGoodsGross", qualifiedByName = "multiply")
    @Mapping(target = "bulkyGoodsNet", source = "bulkyGoodsNet", qualifiedByName = "multiply")
    @Mapping(target = "riskyGoodsGross", source = "riskyGoodsGross", qualifiedByName = "multiply")
    @Mapping(target = "riskyGoodsNet", source = "riskyGoodsNet", qualifiedByName = "multiply")
    @Mapping(target = "discountGross", source = "discountGross", qualifiedByName = "multiply")
    @Mapping(target = "discountNet", source = "discountNet", qualifiedByName = "multiply")
    @Mapping(target = "totalDiscountedGross", source = "unitValues", qualifiedByName = "evaluateTotalDiscountGross")
    @Mapping(target = "totalDiscountedNet", source = "unitValues", qualifiedByName = "evaluateTotalDiscountNet")
    @Mapping(target = "exchangePartValueGross", source = "exchangePartValueGross", qualifiedByName = "multiply")
    @Mapping(target = "exchangePartValueNet", source = "exchangePartValueNet", qualifiedByName = "multiply")
    SumValues toSumValues(UnitValues unitValues, @Context BigDecimal quantity);

    @Mapping(target = "goodsValueGross", source = "goodsGrossValue")
    @Mapping(target = "goodsValueNet", source = "goodsNetValue")
    @Mapping(target = "discountGross", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "discountNet", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "totalDiscountedGross", source = "goodsGrossValue")
    @Mapping(target = "totalDiscountedNet", source = "goodsNetValue")
    SumValues updateByGoodsValue(SumValues sumValues, BigDecimal goodsGrossValue, BigDecimal goodsNetValue);


    @Named("multiply")
    default BigDecimal multiply(BigDecimal value, @Context BigDecimal quantity) {
        return getMultipliedValue(value, quantity);
    }

    @Named("calculateNet")
    default BigDecimal calculateNet(BigDecimal value, @Context BigDecimal taxRate) {
        return getNetValue(value, taxRate);
    }

    @Named("evaluateDiscountGross")
    default BigDecimal evaluateDiscountGross(UnitValues unitValues) {
        return getDiscountResult(unitValues.getGoodsValueGross(), unitValues.getDiscountGross());
    }

    @Named("evaluateDiscountNet")
    default BigDecimal evaluateDiscountNet(UnitValues unitValues) {
        return getDiscountResult(unitValues.getGoodsValueNet(), unitValues.getDiscountNet());
    }

    @Named("evaluateTotalDiscountGross")
    default BigDecimal evaluateTotalDiscountGross(UnitValues unitValues, @Context BigDecimal quantity) {
        return getMultipliedValue(getDiscountResult(unitValues.getGoodsValueGross(), unitValues.getDiscountGross()), quantity);
    }

    @Named("evaluateTotalDiscountNet")
    default BigDecimal evaluateTotalDiscountNet(UnitValues unitValues, @Context BigDecimal quantity) {
        return getMultipliedValue(getDiscountResult(unitValues.getGoodsValueNet(), unitValues.getDiscountNet()), quantity);
    }

    @Named("evaluateOrderDateTime")
    default String evaluateOrderDateTime(String orderDateTime) {
        return DATE_TIME_FORMATTER.format(LocalDateTime.now());
    }
}
