package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.UnitValues;
import lombok.SneakyThrows;
import lombok.val;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapperImpl();

    @Test
    void testToSumValues() {
        val salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        val orderRows = salesOrder.getLatestJson().getOrderRows().get(0);
        val unitValues = UnitValues.builder()
                .goodsValueGross(BigDecimal.valueOf(1))
                .goodsValueNet(BigDecimal.valueOf(2))
                .discountedGross(BigDecimal.valueOf(3))
                .discountedNet(BigDecimal.valueOf(4))
                .discountGross(BigDecimal.valueOf(5))
                .bulkyGoodsGross(BigDecimal.valueOf(6))
                .bulkyGoodsNet(BigDecimal.valueOf(7))
                .depositGross(BigDecimal.valueOf(8))
                .depositNet(BigDecimal.valueOf(9))
                .discountNet(BigDecimal.valueOf(10))
                .exchangePartValueGross(BigDecimal.valueOf(11))
                .exchangePartValueNet(BigDecimal.valueOf(12))
                .riskyGoodsGross(BigDecimal.valueOf(13))
                .riskyGoodsNet(BigDecimal.valueOf(14))
                .build();

        val five = BigDecimal.valueOf(5);
        val sumValues = orderMapper.toSumValues(unitValues, five);
        assertThat(sumValues.getGoodsValueGross()).isEqualTo(unitValues.getGoodsValueGross().multiply(five));
        assertThat(sumValues.getDiscountNet()).isEqualTo(unitValues.getDiscountNet().multiply(five));
        assertThat(sumValues.getDiscountGross()).isEqualTo(unitValues.getDiscountGross().multiply(five));
        assertThat(sumValues.getExchangePartValueGross()).isEqualTo(unitValues.getExchangePartValueGross().multiply(five));
        assertThat(sumValues.getTotalDiscountedNet()).isEqualTo(sumValues.getGoodsValueNet().subtract(sumValues.getDiscountNet()));
        assertThat(sumValues.getTotalDiscountedGross()).isEqualTo(sumValues.getGoodsValueGross().subtract(sumValues.getDiscountGross()));
        assertThat(sumValues.getBulkyGoodsGross()).isEqualTo(unitValues.getBulkyGoodsGross().multiply(five));
        assertThat(sumValues.getGoodsValueNet()).isEqualTo(unitValues.getGoodsValueNet().multiply(five));
        assertThat(sumValues.getBulkyGoodsNet()).isEqualTo(unitValues.getBulkyGoodsNet().multiply(five));
        assertThat(sumValues.getDepositGross()).isEqualTo(unitValues.getDepositGross().multiply(five));
        assertThat(sumValues.getDepositNet()).isEqualTo(unitValues.getDepositNet().multiply(five));
    }

    @Test
    void testToNullSumValues() {
        val salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        val orderRows = salesOrder.getLatestJson().getOrderRows().get(0);
        val unitValues = UnitValues.builder().build();

        val five = BigDecimal.valueOf(5);
        val sumValues = orderMapper.toSumValues(unitValues, five);
        assertThat(sumValues.getGoodsValueGross()).isZero();
        assertThat(sumValues.getDiscountNet()).isZero();
        assertThat(sumValues.getDiscountGross()).isZero();
        assertThat(sumValues.getExchangePartValueGross()).isZero();
        assertThat(sumValues.getTotalDiscountedNet()).isZero();
        assertThat(sumValues.getTotalDiscountedGross()).isZero();
        assertThat(sumValues.getBulkyGoodsGross()).isZero();
        assertThat(sumValues.getGoodsValueNet()).isZero();
        assertThat(sumValues.getBulkyGoodsNet()).isZero();
        assertThat(sumValues.getDepositGross()).isZero();
        assertThat(sumValues.getDepositNet()).isZero();
    }
}