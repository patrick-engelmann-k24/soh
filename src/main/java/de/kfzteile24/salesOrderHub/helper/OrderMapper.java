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

import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.*;

/**
 * @author samet
 */

@Mapper
public interface OrderMapper {
    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    OrderHeader toOrderHeader(OrderHeader orderHeader);

    OrderRows toOrderRow(OrderRows orderRow);

    @Mapping(target = "rrpGross", source = "rrpNet", qualifiedByName = "calculateGross")
    @Mapping(target = "goodsValueGross", source = "goodsValueNet", qualifiedByName = "calculateGross")
    @Mapping(target = "depositGross", source = "depositNet", qualifiedByName = "calculateGross")
    @Mapping(target = "bulkyGoodsGross", source = "bulkyGoodsNet", qualifiedByName = "calculateGross")
    @Mapping(target = "riskyGoodsGross", source = "riskyGoodsNet", qualifiedByName = "calculateGross")
    @Mapping(target = "discountGross", source = "discountGross", qualifiedByName = "calculateGross")
    @Mapping(target = "discountNet", source = "discountNet", qualifiedByName = "calculateGross")
    @Mapping(target = "discountedGross", source = "unitValues", qualifiedByName = "evaluateDiscountGross")
    @Mapping(target = "discountedNet", source = "unitValues", qualifiedByName = "evaluateDiscountNet")
    @Mapping(target = "exchangePartValueGross", source = "exchangePartValueNet", qualifiedByName = "calculateGross")
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

    @Named("calculateGross")
    default BigDecimal calculateGross(BigDecimal value, @Context BigDecimal taxRate) {
        return getGrossValue(value, taxRate);
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
}
