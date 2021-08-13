package de.kfzteile24.salesOrderHub.converter;

import de.kfzteile24.salesOrderHub.dto.order.Rows;
import de.kfzteile24.salesOrderHub.dto.order.rows.SumValues;
import de.kfzteile24.soh.order.dto.Surcharges;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SurchargesConverterTest {
    private final SurchargesConverter surchargesConverter = new SurchargesConverter();

    @Test
    public void surchargesAreCalculatedCorrectly() {
        final Pair<List<Rows>, Surcharges> testData = createRows();
        final var rows = testData.getLeft();
        final var expectedSurcharges = testData.getRight();

        final var actualSurcharges = surchargesConverter.convert(rows);

        assertThat(actualSurcharges).isNotNull();
        assertThat(actualSurcharges.getDepositGross()).isEqualTo(expectedSurcharges.getDepositGross());
        assertThat(actualSurcharges.getDepositNet()).isEqualTo(expectedSurcharges.getDepositNet());
        assertThat(actualSurcharges.getBulkyGoodsGross()).isEqualTo(expectedSurcharges.getBulkyGoodsGross());
        assertThat(actualSurcharges.getBulkyGoodsNet()).isEqualTo(expectedSurcharges.getBulkyGoodsNet());
        assertThat(actualSurcharges.getRiskyGoodsGross()).isEqualTo(expectedSurcharges.getRiskyGoodsGross());
        assertThat(actualSurcharges.getRiskyGoodsNet()).isEqualTo(expectedSurcharges.getRiskyGoodsNet());
    }

    private Pair<List<Rows>, Surcharges> createRows() {
        return Pair.of(
                List.of(createRow(
                        new BigDecimal("1"),
                        new BigDecimal("2"),
                        new BigDecimal("3"),
                        new BigDecimal("4"),
                        new BigDecimal("5"),
                        new BigDecimal("6")
                ),
                createRow(
                        new BigDecimal("10"),
                        new BigDecimal("20"),
                        new BigDecimal("30"),
                        new BigDecimal("40"),
                        new BigDecimal("50"),
                        new BigDecimal("60")
                )),
                Surcharges.builder()
                        .depositGross(new BigDecimal("11"))
                        .depositNet(new BigDecimal("22"))
                        .bulkyGoodsGross(new BigDecimal("33"))
                        .bulkyGoodsNet(new BigDecimal("44"))
                        .riskyGoodsGross(new BigDecimal("55"))
                        .riskyGoodsNet(new BigDecimal("66"))
                        .build());
    }

    private Rows createRow(
            BigDecimal depositGross,
            BigDecimal depositNet,
            BigDecimal bulkyGoodsGross,
            BigDecimal bulkyGoodsNet,
            BigDecimal riskyGoodGross,
            BigDecimal riskyGoodsNet) {

        var sumValues = new SumValues();
        sumValues.setDepositGross(depositGross);
        sumValues.setDepositNet(depositNet);
        sumValues.setBulkyGoodsGross(bulkyGoodsGross);
        sumValues.setBulkyGoodsNet(bulkyGoodsNet);
        sumValues.setRiskyGoodsGross(riskyGoodGross);
        sumValues.setRiskyGoodsNet(riskyGoodsNet);

        var row = new Rows();
        row.setSumValues(sumValues);

        return row;
    }

}