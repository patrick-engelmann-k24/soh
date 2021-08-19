package de.kfzteile24.salesOrderHub.converter;

import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.soh.order.dto.Order;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderJsonConverter implements Converter<OrderJSON, Order> {
    public static final String ORDER_JSON_VERSION = "3.0";

    @NonNull
    private final OrderHeaderConverter orderHeaderConverter;

    @NonNull
    private final OrderRowConverter orderRowConverter;

    @Override
    public Order convert(OrderJSON source) {
        return Order.builder()
                .version(ORDER_JSON_VERSION)
                .orderHeader(orderHeaderConverter.convert(source))
                .orderRows(orderRowConverter.convert(source))
                .build();
    }
}
