package de.kfzteile24.salesOrderHub.services;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesOrderService {

    @Autowired
    private CamundaHelper camundaHelper;

    @Autowired
    private Gson gson;

    @Autowired
    private SalesOrderRepository orderRepository;

    @Autowired
    private InvoiceService invoiceService;

    public SalesOrder updateOrder(final SalesOrder salesOrder) {
        salesOrder.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(salesOrder);
    }

    public SalesOrder updateOrderBillingAddress(SalesOrder salesOrder, Address address) {
        // todo update SalesOrcderInvoice
        salesOrder.getOriginalOrder().getOrderHeader().setBillingAddress(address);
        orderRepository.save(salesOrder);
        return salesOrder;
    }

    public Boolean isOrderBillingAddressChangeable(String orderNumber) {
        final Optional<SalesOrder> order = this.getOrderByOrderNumber(orderNumber);
        if (order.isPresent()) {
            return invoiceService.checkInvoiceExistentForOrder(order.get().getOrderNumber());
        }

        return false;
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
