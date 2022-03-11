package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.dto.sns.SubsequentDeliveryMessage;
import de.kfzteile24.salesOrderHub.dto.sns.subsequent.SubsequentDeliveryItem;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Platform;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderService {

    @NonNull
    private final SalesOrderRepository orderRepository;

    @NonNull
    private final AuditLogRepository auditLogRepository;

    @NonNull
    private final InvoiceService invoiceService;

    public SalesOrder updateOrder(final SalesOrder salesOrder) {
        salesOrder.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(salesOrder);
    }

    public Optional<SalesOrder> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.getOrderByOrderNumber(orderNumber);
    }

    @Transactional
    public SalesOrder createSalesOrder(SalesOrder salesOrder) {
        salesOrder.setRecurringOrder(isRecurringOrder(salesOrder));

        final Set<SalesOrderInvoice> invoices = invoiceService.getInvoicesByOrderNumber(salesOrder.getOrderNumber());
        final Set<SalesOrderInvoice> updatedInvoices = invoices.stream()
                .map(invoice -> invoiceService.addSalesOrderToInvoice(salesOrder, invoice))
                .collect(Collectors.toSet());

        salesOrder.setSalesOrderInvoiceList(updatedInvoices);
        return save(salesOrder, ORDER_CREATED);
    }

    @Transactional
    public SalesOrder save(SalesOrder order, Action action) {
        final var storedOrder = orderRepository.save(order);

        final var auditLog = AuditLog.builder()
                .salesOrderId(storedOrder.getId())
                .action(action)
                .data(order.getLatestJson())
                .build();

        auditLogRepository.save(auditLog);

        return storedOrder;
    }

    /**
     * checks if there is any order in the past for this customer. If yes then sets the status
     * of the order to recurring.
     */
    public boolean isRecurringOrder(SalesOrder salesOrder) {
        return orderRepository.countByCustomerEmail(salesOrder.getCustomerEmail()) > 0;
    }

    @Transactional
    public SalesOrder createSalesOrderForSubsequentDelivery(SubsequentDeliveryMessage subsequent, String newOrderNumber) {
        Set<String> skuSet = subsequent.getItems().stream()
                .map(SubsequentDeliveryItem::getSku)
                .collect(Collectors.toSet());
        Order order = getLatestOrderWithFilteredSkus(subsequent.getOrderNumber(), skuSet);
        order.getOrderHeader().setPlatform(Platform.SOH);

        return SalesOrder.builder()
                .orderNumber(newOrderNumber)
                .orderGroupId(subsequent.getOrderNumber())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .customerEmail(order.getOrderHeader().getCustomer().getCustomerEmail())
                .originalOrder(order)
                .latestJson(order)
                .build();
    }
    protected Order getLatestOrderWithFilteredSkus(String orderNumber, Set<String> acceptableSkuSet) {
        var originalSalesOrder = getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        var order = originalSalesOrder.getLatestJson();
        var subsequentOrder = Order.builder()
                .version(order.getVersion())
                .orderHeader(order.getOrderHeader())
                .orderRows(filterOrderRows(order, acceptableSkuSet)).build();

        if (subsequentOrder.getOrderRows().isEmpty()) {
            String skuList = String.join(",", acceptableSkuSet);
            log.error(MessageFormat.format(
                    "Order Row ID NotFoundException: " +
                            "There is no order row id matching with the ones in subsequent delivery note msg. " +
                    "Order number: {0}, Subsequent Delivery Note Sku Items: {1}", orderNumber, skuList));
            throw new SalesOrderNotFoundException(MessageFormat.format("{0} with any order row for subsequent delivery",
                    orderNumber));
        }

        return subsequentOrder;
    }

    protected List<OrderRows> filterOrderRows(Order order, Set<String> skuSet) {

        // check if there is any mismatch
        skuSet.stream()
                .filter(sku -> order.getOrderRows().stream().noneMatch(row -> row.getSku().equals(sku)))
                .forEach(sku ->
                        log.error(MessageFormat.format(
                        "Order Row ID MismatchingError: " +
                                "The order row id, {0}, given in subsequent delivery note msg, " +
                                "is not matching with any of the order row id in the original order with " +
                                "order number: {1}", sku, order.getOrderHeader().getOrderNumber())));

        return order.getOrderRows().stream()
                .filter(row -> skuSet.contains(row.getSku()))
                .collect(Collectors.toList());
    }
}
