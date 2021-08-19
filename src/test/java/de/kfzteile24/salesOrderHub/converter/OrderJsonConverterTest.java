package de.kfzteile24.salesOrderHub.converter;

import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.soh.order.dto.OrderHeader;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderJsonConverterTest {

    @Mock
    private OrderHeaderConverter orderHeaderConverter;

    @Mock
    private OrderRowConverter orderRowConverter;

    @InjectMocks
    private OrderJsonConverter orderJsonConverter;

    @Test
    public void theOrderJsonMainStructureIsConvertedCorrectly() {
        final var orderHeader = OrderHeader.builder().orderId(UUID.randomUUID()).build();
        when(orderHeaderConverter.convert(any())).thenReturn(orderHeader);

        final var orderRows = List.of(OrderRows.builder().rowKey(123).build());
        when(orderRowConverter.convert(any())).thenReturn(orderRows);

        var orderJson = new OrderJSON();
        final var convertedOrder = orderJsonConverter.convert(orderJson);

        assertThat(convertedOrder).isNotNull();
        assertThat(convertedOrder.getVersion()).isEqualTo(OrderJsonConverter.ORDER_JSON_VERSION);
        assertThat(convertedOrder.getOrderHeader().getOrderId()).isEqualTo(orderHeader.getOrderId());
        assertThat(orderRows.get(0).getRowKey()).isEqualTo(orderRows.get(0).getRowKey());

        verify(orderHeaderConverter).convert(orderJson);
        verify(orderRowConverter).convert(orderJson);
    }

}