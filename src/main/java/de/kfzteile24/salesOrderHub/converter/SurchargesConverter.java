package de.kfzteile24.salesOrderHub.converter;


import de.kfzteile24.salesOrderHub.dto.order.Rows;
import de.kfzteile24.soh.order.dto.Surcharges;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

@Component
public class SurchargesConverter implements Converter<Collection<Rows>, Surcharges> {

    @Override
    public Surcharges convert(Collection<Rows> rows) {
        return Surcharges.builder()
                .depositGross(sum(rows, row -> row.getSumValues().getDepositGross()))
                .depositNet(sum(rows, row -> row.getSumValues().getDepositNet()))
                .bulkyGoodsGross(sum(rows, row -> row.getSumValues().getBulkyGoodsGross()))
                .bulkyGoodsNet(sum(rows, row -> row.getSumValues().getBulkyGoodsNet()))
                .riskyGoodsGross(sum(rows, row -> row.getSumValues().getRiskyGoodsGross()))
                .riskyGoodsNet(sum(rows, row -> row.getSumValues().getRiskyGoodsNet()))
                .build();
    }

    private BigDecimal sum(Collection<Rows> rows, Function<Rows,BigDecimal> getValue) {
       return rows.stream()
               .map(getValue)
               .filter(Objects::nonNull)
               .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
