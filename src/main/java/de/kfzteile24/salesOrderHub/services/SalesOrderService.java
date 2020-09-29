package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesOrderService {

    @NotNull
    private final SalesOrderRepository orderRepository;

    void updateOrder(final SalesOrder salesOrder) {
        salesOrder.setUpdatedAt(new Date());
        orderRepository.save(salesOrder);
    }

    public SalesOrder createOrder(/* todo: GSON class?*/String orderNumber) {
        final SalesOrder salesOrder = SalesOrder.builder()
                .orderNumber(orderNumber)
                .originalOrder("{\"orderNumber\": 1234}")
                .salesLocale("DE_de")
                .build();

        return this.save(salesOrder);
    }

    public SalesOrder createOrder(SalesOrder salesOrder) {
        return orderRepository.save(salesOrder);
    }

    public Optional<SalesOrder> findById(UUID id) {
        return orderRepository.findById(id);
    }

    public Optional<SalesOrder> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.getOrderByOrderNumber(orderNumber);
    }

    public Optional<SalesOrder> getOrderByProcessId(UUID processId) {
        return orderRepository.getOrderByProcessId(processId);
    }

    public SalesOrder save(SalesOrder order) {
        return this.orderRepository.save(order);
    }

}
