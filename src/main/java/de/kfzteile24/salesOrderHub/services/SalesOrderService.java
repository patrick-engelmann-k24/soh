package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getGrossValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.isNotNullAndEqual;
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

    public Optional<SalesOrder> getOrderById(UUID salesOrderId) {
        return orderRepository.findById(salesOrderId);
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
    public SalesOrder createSalesOrderForInvoice(CoreSalesInvoiceCreatedMessage salesInvoiceCreatedMessage,
                                                 SalesOrder originalSalesOrder,
                                                 String newOrderNumber) {

        CoreSalesInvoiceHeader salesInvoiceHeader = salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        Order order = createOrderForSubsequentSalesOrder(salesInvoiceCreatedMessage, originalSalesOrder);
        order.getOrderHeader().setPlatform(Platform.SOH);
        order.getOrderHeader().setOrderNumber(newOrderNumber);
        order.getOrderHeader().setDocumentRefNumber(salesInvoiceHeader.getInvoiceNumber());
        salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(newOrderNumber);
        var shippingCostDocumentLine =  salesInvoiceHeader.getInvoiceLines().stream()
                .filter(CoreSalesFinancialDocumentLine::getIsShippingCost).findFirst().orElse(null);
        recalculateTotals(order, shippingCostDocumentLine);

        var salesOrder = SalesOrder.builder()
                .orderNumber(newOrderNumber)
                .orderGroupId(order.getOrderHeader().getOrderGroupId())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .customerEmail(order.getOrderHeader().getCustomer().getCustomerEmail())
                .originalOrder(order)
                .latestJson(order)
                .invoiceEvent(salesInvoiceCreatedMessage)
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

    protected Order createOrderForSubsequentSalesOrder(CoreSalesInvoiceCreatedMessage coreSalesInvoiceCreatedMessage,
                                                       SalesOrder originalSalesOrder) {
        CoreSalesInvoiceHeader salesInvoiceHeader = coreSalesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        var items = salesInvoiceHeader.getInvoiceLines();
        List<OrderRows> orderRows = new ArrayList<>();
        var lastRowKey = orderUtil.getLastRowKey(originalSalesOrder);
        for (CoreSalesFinancialDocumentLine item : items) {
            if (!item.getIsShippingCost()) {
                orderRows.add(orderUtil.createNewOrderRow(item, originalSalesOrder, lastRowKey));
                lastRowKey = orderUtil.updateLastRowKey(originalSalesOrder, item.getItemNumber(), lastRowKey);
            }
        }
        orderRows = orderRows.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        var version = originalSalesOrder.getLatestJson().getVersion();
        var orderHeader = originalSalesOrder.getLatestJson().getOrderHeader();
        return Order.builder()
                .version(version)
                .orderHeader(OrderMapper.INSTANCE.toOrderHeader(orderHeader))
                .orderRows(orderRows)
                .build();
    }

    public boolean isFullyMatchedWithOriginalOrder(SalesOrder originalSalesOrder,
                                                   List<CoreSalesFinancialDocumentLine> items) {
        // Check if there is no other sales order namely subsequent order
        var ordersByOrderGroupId = getOrderByOrderGroupId(originalSalesOrder.getOrderGroupId());
        if (ordersByOrderGroupId.size() > 1) {
            return false;
        }

        // Check if all sku names are matching with original sales order
        var orderRows = originalSalesOrder.getLatestJson().getOrderRows();
        var skuList = orderRows.stream().map(OrderRows::getSku).collect(Collectors.toSet());
        if (!items.stream().allMatch(row -> skuList.contains(row.getItemNumber()))) {
            return false;
        }

        // Check if all order rows have same values within the invoice event
        for (OrderRows row : orderRows) {
            var item = items.stream().filter(each -> row.getSku().equals(each.getItemNumber())).findFirst().orElse(null);
            if (item == null)
                return false;
            if (!isNotNullAndEqual(item.getQuantity(), row.getQuantity()))
                return false;
            if (!isNotNullAndEqual(item.getTaxRate(), row.getTaxRate()))
                return false;
            if (!isNotNullAndEqual(item.getUnitNetAmount(), row.getUnitValues().getGoodsValueNet()))
                return false;
        }
        return true;
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

    private void recalculateTotals(Order order, CoreSalesFinancialDocumentLine shippingCostLine) {

        List<OrderRows> orderRows = order.getOrderRows();
        List<SumValues> sumValues = orderRows.stream().map(OrderRows::getSumValues).collect(Collectors.toList());
        BigDecimal goodsTotalGross = getSumValue(SumValues::getGoodsValueGross, sumValues);
        BigDecimal goodsTotalNet = getSumValue(SumValues::getGoodsValueNet, sumValues);
        BigDecimal totalDiscountGross = getSumValue(SumValues::getDiscountGross, sumValues);
        BigDecimal totalDiscountNet = getSumValue(SumValues::getDiscountNet, sumValues);
        BigDecimal shippingCostNet = shippingCostLine != null ? shippingCostLine.getUnitNetAmount() : BigDecimal.ZERO;
        BigDecimal shippingCostGross = shippingCostLine != null ?
                getGrossValue(shippingCostLine.getUnitNetAmount(), shippingCostLine.getTaxRate()) : BigDecimal.ZERO;
        BigDecimal grandTotalGross = goodsTotalGross.subtract(totalDiscountGross).add(shippingCostGross);
        BigDecimal grantTotalNet = goodsTotalNet.subtract(totalDiscountNet).add(shippingCostNet);

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
                .shippingCostGross(shippingCostGross)
                .shippingCostNet(shippingCostNet)
                .build();

        if (shippingCostLine != null) {
            BigDecimal fullTaxValue = grandTotalGross.subtract(grantTotalNet);
            BigDecimal sumTaxValues = totals.getGrandTotalTaxes().stream()
                    .map(GrandTotalTaxes::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal taxValueToAdd = fullTaxValue.subtract(sumTaxValues);
            totals.getGrandTotalTaxes().stream().findFirst().
                    ifPresent(tax -> tax.setValue(tax.getValue().add(taxValueToAdd)));
        }

        order.getOrderHeader().setTotals(totals);
    }

    public List<GrandTotalTaxes> calculateGrandTotalTaxes(Order order) {

        List<GrandTotalTaxes> oldGrandTotalTaxesList = order.getOrderHeader().getTotals().getGrandTotalTaxes();
        List<GrandTotalTaxes> grandTotalTaxesList = order.getOrderRows().stream()
                .map(this::createGrandTotalTaxesFromOrderRow).distinct().collect(Collectors.toList());

        for (OrderRows orderRow : order.getOrderRows()) {
            BigDecimal taxValue = orderRow.getSumValues().getGoodsValueGross()
                    .subtract(orderRow.getSumValues().getGoodsValueNet());
            var grandTotalTax = oldGrandTotalTaxesList.stream()
                    .filter(tax -> tax.getRate().equals(orderRow.getTaxRate()))
                    .findFirst().orElse(null);

            grandTotalTaxesList.stream()
                    .filter(tax -> tax.getRate().equals(orderRow.getTaxRate())).findFirst()
                    .ifPresent(grandTotalTaxes -> {
                        grandTotalTaxes.setValue(grandTotalTaxes.getValue().add(taxValue));
                        grandTotalTaxes.setType(grandTotalTax != null ? grandTotalTax.getType() : null);
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

    public String createOrderNumberInSOH(String orderNumber, String reference) {
        return orderUtil.createOrderNumberInSOH(orderNumber, reference);
    }
}
