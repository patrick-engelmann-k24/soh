package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.dto.sns.SubsequentDeliveryMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderMapper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Platform;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Surcharges;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static java.text.MessageFormat.format;

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

    @NonNull
    private final OrderUtil orderUtil;

    public SalesOrder updateOrder(final SalesOrder salesOrder) {
        salesOrder.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(salesOrder);
    }

    @Transactional(readOnly = true)
    public Optional<SalesOrder> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.getOrderByOrderNumber(orderNumber);
    }

    public List<SalesOrder> getOrderByOrderGroupId(String orderGroupId) {
        return orderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(orderGroupId);
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
        Order order = createOrderFromSubsequentDelivery(subsequent);
        order.getOrderHeader().setPlatform(Platform.SOH);
        recalculateOrder(order);

        var salesOrder = SalesOrder.builder()
                .orderNumber(newOrderNumber)
                .orderGroupId(subsequent.getOrderNumber())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .customerEmail(order.getOrderHeader().getCustomer().getCustomerEmail())
                .originalOrder(order)
                .latestJson(order)
                .build();
        return createSalesOrder(salesOrder);
    }


    @Transactional
    public SalesOrder addSalesOrderInvoice(SalesOrder salesOrder, SalesOrderInvoice salesOrderInvoice) {
        salesOrder.getLatestJson()
                .getOrderHeader()
                .setDocumentRefNumber(salesOrder.getOrderNumber());
        salesOrderInvoice.setSalesOrder(salesOrder);
        salesOrder.getSalesOrderInvoiceList().add(salesOrderInvoice);
        return save(salesOrder, INVOICE_RECEIVED);
    }

    public SalesOrder addSalesOrderReturn(SalesOrder salesOrder, SalesOrderReturn salesOrderReturn) {
        salesOrderReturn.setSalesOrder(salesOrder);
        salesOrder.getSalesOrderReturnList().add(salesOrderReturn);
        return save(salesOrder, Action.RETURN_ORDER_CREATED);
    }

    public SalesOrder findLastOrderByOrderGroupId(String orderGroupId) {
        return getOrderByOrderGroupId(orderGroupId)
                .stream()
                .findFirst() // the most recent one
                .orElseThrow(() -> new SalesOrderNotFoundCustomException(
                        format("for the given order group id {0}", orderGroupId)));
    }

    protected Order createOrderFromSubsequentDelivery(SubsequentDeliveryMessage subsequent) {
        var originalSalesOrder = getOrderByOrderNumber(subsequent.getOrderNumber())
                .orElseThrow(() -> new SalesOrderNotFoundException(subsequent.getOrderNumber()));

        var orderRows = subsequent.getItems().stream()
                .map(item -> orderUtil.createOrderFromOriginalSalesOrder(
                        originalSalesOrder,
                        item,
                        orderUtil.getLastRowKey(originalSalesOrder)))
                .collect(Collectors.toList());

        var order = originalSalesOrder.getLatestJson();
        return Order.builder()
                .version(order.getVersion())
                .orderHeader(OrderMapper.INSTANCE.toOrderHeader(order.getOrderHeader()))
                .orderRows(orderRows)
                .build();
    }

    public List<String> getOrderNumberListByOrderGroupId(String orderGroupId, String orderItemSku) {
        var fetchedSalesOrders = getOrderByOrderGroupId(orderGroupId);

        if (CollectionUtils.isNotEmpty(fetchedSalesOrders)) {
            return filterBySku(orderGroupId, orderItemSku, fetchedSalesOrders);
        }

        throw new SalesOrderNotFoundCustomException(
                format("for the given order group id {0}", orderGroupId));
    }

    public List<String> getOrderNumberListByOrderGroupIdAndFilterNotCancelled(String orderGroupId, String orderItemSku) {
        var fetchedSalesOrders = getOrderByOrderGroupId(orderGroupId);

        if (CollectionUtils.isNotEmpty(fetchedSalesOrders)) {
            return filterBySkuAndIsCancelled(orderGroupId, orderItemSku, fetchedSalesOrders);
        }

        throw new SalesOrderNotFoundCustomException(
                format("for the given order group id {0}", orderGroupId));
    }

    protected List<String> filterBySku(String orderGroupId, String orderItemSku, List<SalesOrder> fetchedSalesOrders) {
        var foundSalesOrders = fetchedSalesOrders.stream()
                .filter(salesOrder -> salesOrder.getLatestJson().getOrderRows().stream()
                        .anyMatch(row -> row.getSku().equals(orderItemSku)))
                .collect(Collectors.toList());

        if (foundSalesOrders.isEmpty()) {
            throw new SalesOrderNotFoundCustomException(
                    format("for the given order group id {0} and given order row sku number {1}",
                            orderGroupId,
                            orderItemSku));
        }

        return foundSalesOrders.stream().map(SalesOrder::getOrderNumber).distinct().collect(Collectors.toList());
    }

    protected List<String> filterBySkuAndIsCancelled(String orderGroupId, String orderItemSku, List<SalesOrder> fetchedSalesOrders) {
        var foundSalesOrders = fetchedSalesOrders.stream()
                .filter(salesOrder -> salesOrder.getLatestJson().getOrderRows().stream()
                        .anyMatch(row -> row.getSku().equals(orderItemSku)))
                .filter(salesOrder -> salesOrder.getLatestJson().getOrderRows().stream()
                        .noneMatch(OrderRows::getIsCancelled))
                .collect(Collectors.toList());

        if (foundSalesOrders.isEmpty()) {
            throw new SalesOrderNotFoundCustomException(
                    format("for the given order group id {0} and given order row sku number {1}",
                            orderGroupId,
                            orderItemSku));
        }

        return foundSalesOrders.stream().map(SalesOrder::getOrderNumber).distinct().collect(Collectors.toList());
    }

    private void recalculateOrder(Order order) {

        List<OrderRows> orderRows = order.getOrderRows();
        List<SumValues> sumValues = orderRows.stream().map(OrderRows::getSumValues).collect(Collectors.toList());
        BigDecimal goodsTotalGross = getSumValue(SumValues::getGoodsValueGross, sumValues);
        BigDecimal goodsTotalNet = getSumValue(SumValues::getGoodsValueNet, sumValues);
        BigDecimal totalDiscountGross = getSumValue(SumValues::getDiscountGross, sumValues);
        BigDecimal totalDiscountNet = getSumValue(SumValues::getDiscountNet, sumValues);
        BigDecimal grandTotalGross = goodsTotalGross.subtract(totalDiscountGross);
        BigDecimal grantTotalNet = goodsTotalNet.subtract(totalDiscountNet);

        Totals totals = Totals.builder()
                .goodsTotalGross(goodsTotalGross)
                .goodsTotalNet(goodsTotalNet)
                .totalDiscountGross(totalDiscountGross)
                .totalDiscountNet(totalDiscountNet)
                .grandTotalGross(grandTotalGross)
                .grandTotalNet(grantTotalNet)
                .paymentTotal(grandTotalGross)
                .grandTotalTaxes(calculateGrandTotalTaxes(order))
                .surcharges(Surcharges.builder().build())
                .shippingCostGross(null)
                .shippingCostNet(null)
                .build();

        order.getOrderHeader().setTotals(totals);
    }

    private List<GrandTotalTaxes> calculateGrandTotalTaxes(Order order) {

        List<GrandTotalTaxes> oldGrandTotalTaxesList = order.getOrderHeader().getTotals().getGrandTotalTaxes();
        List<GrandTotalTaxes> grandTotalTaxesList = order.getOrderRows().stream()
                .map(this::createGrandTotalTaxesFromOrderRow).distinct().collect(Collectors.toList());

        for (OrderRows orderRow : order.getOrderRows()) {
            BigDecimal taxValue = orderRow.getSumValues().getGoodsValueGross()
                    .subtract(orderRow.getSumValues().getGoodsValueNet());
            String taxType = oldGrandTotalTaxesList.stream()
                    .filter(tax -> tax.getRate().equals(orderRow.getTaxRate()))
                    .map(GrandTotalTaxes::getType).findFirst().orElse(null);

            grandTotalTaxesList.stream()
                    .filter(tax -> tax.getRate().equals(orderRow.getTaxRate())).findFirst()
                    .ifPresent(grandTotalTaxes -> {
                        grandTotalTaxes.setValue(grandTotalTaxes.getValue().add(taxValue));
                        grandTotalTaxes.setType(taxType);
                    });
        }
        return grandTotalTaxesList;
    }

    private GrandTotalTaxes createGrandTotalTaxesFromOrderRow(OrderRows row) {

        return GrandTotalTaxes.builder()
                .rate(row.getTaxRate())
                .value(BigDecimal.ZERO)
                .build();
    }
}
