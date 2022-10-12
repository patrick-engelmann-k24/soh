package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Test;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;

class SalesOrderMapperTest {

    private final SalesOrderMapper salesOrderMapper = new SalesOrderMapperImpl();

    @SneakyThrows
    @Test
    void testMapOrderToSalesOrder() {

        var message = getObjectByResource("ecpOrderMessage.json", Order.class);

        var salesOrder = salesOrderMapper.map(message);

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(salesOrder.getOrderNumber()).isEqualTo(message.getOrderHeader().getOrderNumber());
            softly.assertThat(salesOrder.getLatestJson()).isEqualTo(message);
            softly.assertThat(salesOrder.getVersion()).isEqualTo(3L);
            softly.assertThat(salesOrder.getOriginalOrder()).isEqualTo(message);
            softly.assertThat(salesOrder.getCustomerEmail()).isEqualTo(message.getOrderHeader().getCustomer().getCustomerEmail());
            softly.assertThat(salesOrder.getOrderGroupId()).isEqualTo(message.getOrderHeader().getOrderGroupId());
            softly.assertThat(salesOrder.getSalesChannel()).isEqualTo(message.getOrderHeader().getSalesChannel());
        }
    }
}