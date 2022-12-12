package de.kfzteile24.salesOrderHub.services.returnorder;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RegularReturnOrderHandler implements ReturnOrderCreator {

    private final SalesOrderService salesOrderService;

    @Override
    public List<SalesOrder> getSalesOrderList(String orderGroupId) {
        return salesOrderService.getOrderByOrderGroupId(orderGroupId)
                .stream()
                .sorted(Comparator.comparing(SalesOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}
