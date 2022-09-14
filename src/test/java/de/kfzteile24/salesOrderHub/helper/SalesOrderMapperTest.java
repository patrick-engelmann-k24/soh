package de.kfzteile24.salesOrderHub.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

class SalesOrderMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    private final SalesOrderMapper salesOrderMapper = new SalesOrderMapperImpl();

    @SneakyThrows
    @Test
    void testMapOrderToSalesOrder() {

        String rawMessage = readResource("examples/ecpOrderMessage.json");
        SqsMessage sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        MessageWrapper<Order> messageWrapper = MessageWrapper.<Order>builder()
                .sqsMessage(sqsMessage)
                .message(objectMapper.readValue(sqsMessage.getBody(), Order.class))
                .rawMessage(rawMessage)
                .build();
        var order = messageWrapper.getMessage();

        var salesOrder = salesOrderMapper.map(order);

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(salesOrder.getOrderNumber()).isEqualTo(order.getOrderHeader().getOrderNumber());
            softly.assertThat(salesOrder.getLatestJson()).isEqualTo(order);
            softly.assertThat(salesOrder.getVersion()).isEqualTo(3L);
            softly.assertThat(salesOrder.getOriginalOrder()).isEqualTo(order);
            softly.assertThat(salesOrder.getCustomerEmail()).isEqualTo(order.getOrderHeader().getCustomer().getCustomerEmail());
            softly.assertThat(salesOrder.getOrderGroupId()).isEqualTo(order.getOrderHeader().getOrderGroupId());
            softly.assertThat(salesOrder.getSalesChannel()).isEqualTo(order.getOrderHeader().getSalesChannel());
        }
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return FileUtil.readResource(getClass(), path);
    }
}