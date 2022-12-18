package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.CustomerSegment;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SubsequentSalesOrderCreationHelper;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Payments;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Surcharges;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.CustomerSegment.B2B;
import static de.kfzteile24.salesOrderHub.constants.CustomerSegment.DIRECT_DELIVERY;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.ORDER_NUMBER_SEPARATOR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.isNotNullAndEqual;
import static de.kfzteile24.salesOrderHub.helper.PaymentUtil.updatePaymentProvider;
import static de.kfzteile24.salesOrderHub.helper.SubsequentSalesOrderCreationHelper.buildSubsequentSalesOrder;
import static de.kfzteile24.soh.order.dto.CustomerType.BUSINESS;
import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

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

    @NonNull
    private final SubsequentSalesOrderCreationHelper subsequentOrderHelper;

    @Transactional
    public SalesOrder updateProcessInstanceId(String orderNumber, String processInstanceId) {
        Optional<SalesOrder> salesOrderOptional = getOrderByOrderNumber(orderNumber);
        if (salesOrderOptional.isPresent()) {
            SalesOrder salesOrder = salesOrderOptional.get();

            salesOrder.setProcessId(processInstanceId);
            return updateOrder(salesOrder);
        }
        throw new SalesOrderNotFoundException(format("for order {0}", orderNumber));
    }

    @Transactional
    public SalesOrder updateOrder(final SalesOrder salesOrder) {
        salesOrder.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(salesOrder);
    }

    @Transactional(readOnly = true)
    public Optional<SalesOrder> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.getOrderByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    public boolean checkOrderNotExists(final String orderNumber) {
        if (getOrderByOrderNumber(orderNumber).isPresent()) {
            log.warn("The following order won't be processed because it exists in SOH system already from another source. " +
                    "Order Number: {}", orderNumber);
            return false;
        }
        return true;
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

        var order = createOrderForSubsequentSalesOrder(salesInvoiceCreatedMessage, originalSalesOrder, newOrderNumber);
        var salesInvoiceHeader = salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        salesInvoiceHeader.setOrderNumber(newOrderNumber);
        salesInvoiceHeader.setOrderGroupId(order.getOrderHeader().getOrderGroupId());
        var shippingCostDocumentLine = salesInvoiceHeader.getInvoiceLines().stream()
                .filter(CoreSalesFinancialDocumentLine::getIsShippingCost).findFirst().orElse(null);
        recalculateTotals(order, shippingCostDocumentLine);

        var subsequentOrder = buildSubsequentSalesOrder(order, newOrderNumber);
        subsequentOrder.setInvoiceEvent(salesInvoiceCreatedMessage);
        return createSalesOrder(subsequentOrder);
    }

    protected Order createOrderForSubsequentSalesOrder(CoreSalesInvoiceCreatedMessage coreSalesInvoiceCreatedMessage,
                                                       SalesOrder originalSalesOrder,
                                                       String newOrderNumber) {
        var salesInvoiceHeader = coreSalesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        var items = salesInvoiceHeader.getInvoiceLines();
        List<OrderRows> orderRows = new ArrayList<>();
        var lastRowKey = new AtomicInteger(orderUtil.getLastRowKey(originalSalesOrder));
        for (CoreSalesFinancialDocumentLine item : items) {
            if (Boolean.FALSE.equals(item.getIsShippingCost())) {
                orderRows.add(orderUtil.createNewOrderRow(item, List.of(originalSalesOrder), lastRowKey));
            }
        }
        orderRows = orderRows.stream()
                .filter(Objects::nonNull)
                .collect(toList());

        var invoiceNumber = salesInvoiceHeader.getInvoiceNumber();
        return Order.builder()
                .version(originalSalesOrder.getLatestJson().getVersion())
                .orderHeader(subsequentOrderHelper.createOrderHeader(originalSalesOrder, newOrderNumber, invoiceNumber))
                .orderRows(orderRows)
                .build();
    }

    @Transactional
    public SalesOrder cancelOrder(String orderNumber) {
        var salesOrder = getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        log.info("Order with order number: {} is being fully cancelled", salesOrder.getOrderNumber());
        salesOrder.setCancelled(true);
        return save(salesOrder, Action.ORDER_CANCELLED);
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
        if (!items.stream().filter(item -> !item.getIsShippingCost()).allMatch(row -> skuList.contains(row.getItemNumber()))) {
            return false;
        }

        // Check if all order rows have same values within the invoice event
        for (OrderRows row : orderRows) {
            var item = items.stream()
                    .filter(each -> !each.getIsShippingCost())
                    .filter(each -> row.getSku().equals(each.getItemNumber())).findFirst().orElse(null);
            if (item == null)
                return false;
            if (!isNotNullAndEqual(item.getQuantity(), row.getQuantity()))
                return false;
            if (!isNotNullAndEqual(item.getTaxRate(), row.getTaxRate()))
                return false;
            if (!isNotNullAndEqual(item.getUnitNetAmount(), row.getUnitValues().getGoodsValueNet()))
                return false;
        }

        var shippingCostDocumentLine = items.stream()
                .filter(CoreSalesFinancialDocumentLine::getIsShippingCost).findFirst().orElse(null);

        return isShippingCostNetMatch(originalSalesOrder, shippingCostDocumentLine)
                && isShippingCostGrossMatch(originalSalesOrder, shippingCostDocumentLine);
    }

    @Transactional
    public boolean isFullyMatched(List<String> skuList, String orderNumber) {
        var salesOrder = getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        return salesOrder.getLatestJson().getOrderRows().stream().allMatch(row -> skuList.contains(row.getSku()));
    }

    private boolean isShippingCostNetMatch(SalesOrder originalSalesOrder, CoreSalesFinancialDocumentLine shippingCostDocumentLine) {
        var shippingCostNet = shippingCostDocumentLine != null
                ? shippingCostDocumentLine.getUnitNetAmount() : BigDecimal.ZERO;
        var orderHeaderShippingCostNet = originalSalesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostNet();
        return isNotNullAndEqual(shippingCostNet != null ? shippingCostNet : BigDecimal.ZERO, orderHeaderShippingCostNet);
    }

    private boolean isShippingCostGrossMatch(SalesOrder originalSalesOrder, CoreSalesFinancialDocumentLine shippingCostDocumentLine) {
        var shippingCostGross = shippingCostDocumentLine != null
                ? shippingCostDocumentLine.getUnitGrossAmount() : BigDecimal.ZERO;
        var orderHeaderShippingCostGross = originalSalesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostGross();
        return isNotNullAndEqual(shippingCostGross != null ? shippingCostGross : BigDecimal.ZERO, orderHeaderShippingCostGross);
    }

    public List<String> getOrderNumberListByOrderGroupIdAndFilterNotCancelled(String orderGroupId, String orderItemSku) {
        var fetchedSalesOrders = getOrderByOrderGroupId(orderGroupId);

        if (CollectionUtils.isNotEmpty(fetchedSalesOrders)) {
            return filterBySkuAndIsCancelled(orderGroupId, orderItemSku, fetchedSalesOrders);
        }

        throw new SalesOrderNotFoundCustomException(
                format("for the given order group id {0}", orderGroupId));
    }

    protected List<String> filterBySkuAndIsCancelled(String orderGroupId, String orderItemSku, List<SalesOrder> fetchedSalesOrders) {
        var foundSalesOrders = fetchedSalesOrders.stream()
                .filter(salesOrder -> salesOrder.getLatestJson().getOrderRows().stream()
                        .anyMatch(row -> row.getSku().equals(orderItemSku)))
                .filter(salesOrder -> salesOrder.getLatestJson().getOrderRows().stream()
                        .noneMatch(OrderRows::getIsCancelled))
                .collect(toList());

        if (foundSalesOrders.isEmpty()) {
            throw new SalesOrderNotFoundCustomException(
                    format("for the given order group id {0} and given order row sku number {1}",
                            orderGroupId,
                            orderItemSku));
        }

        return foundSalesOrders.stream().map(SalesOrder::getOrderNumber).distinct().collect(toList());
    }

    public void recalculateTotals(Order order, CoreSalesFinancialDocumentLine shippingCostLine) {
        BigDecimal shippingCostNet = shippingCostLine != null ? shippingCostLine.getLineNetAmount() : BigDecimal.ZERO;
        BigDecimal shippingCostGross = shippingCostLine != null ? shippingCostLine.getLineGrossAmount() : BigDecimal.ZERO;
        recalculateTotals(order, shippingCostNet, shippingCostGross, shippingCostLine != null);
    }

    public void recalculateTotals(Order order, BigDecimal shippingCostNet, BigDecimal shippingCostGross, boolean shippingCostLine) {

        List<OrderRows> orderRows = order.getOrderRows();
        List<SumValues> sumValues = orderRows.stream().map(OrderRows::getSumValues).collect(toList());
        BigDecimal goodsTotalGross = getSumValue(SumValues::getGoodsValueGross, sumValues);
        BigDecimal goodsTotalNet = getSumValue(SumValues::getGoodsValueNet, sumValues);
        BigDecimal totalDiscountGross = getSumValue(SumValues::getDiscountGross, sumValues);
        BigDecimal totalDiscountNet = getSumValue(SumValues::getDiscountNet, sumValues);
        BigDecimal grandTotalGross = goodsTotalGross.subtract(totalDiscountGross).add(shippingCostGross);
        BigDecimal grandTotalNet = goodsTotalNet.subtract(totalDiscountNet).add(shippingCostNet);

        Totals totals = Totals.builder()
                .goodsTotalGross(goodsTotalGross)
                .goodsTotalNet(goodsTotalNet)
                .totalDiscountGross(totalDiscountGross)
                .totalDiscountNet(totalDiscountNet)
                .grandTotalGross(grandTotalGross)
                .grandTotalNet(grandTotalNet)
                .paymentTotal(grandTotalGross)
                .grandTotalTaxes(calculateGrandTotalTaxes(order))
                .surcharges(Surcharges.builder().build())
                .shippingCostGross(shippingCostGross)
                .shippingCostNet(shippingCostNet)
                .build();

        if (shippingCostLine) {
            BigDecimal shippingTaxToAdd = shippingCostGross.subtract(shippingCostNet);
            totals.getGrandTotalTaxes().stream().findFirst().
                    ifPresent(tax -> tax.setValue(tax.getValue().add(shippingTaxToAdd)));
        }

        order.getOrderHeader().setTotals(totals);
        orderUtil.removeInvalidGrandTotalTaxes(order);
    }

    public List<GrandTotalTaxes> calculateGrandTotalTaxes(Order order) {

        List<GrandTotalTaxes> oldGrandTotalTaxesList = order.getOrderHeader().getTotals().getGrandTotalTaxes();
        List<GrandTotalTaxes> grandTotalTaxesList = order.getOrderRows().stream()
                .map(this::createGrandTotalTaxesFromOrderRow).distinct().collect(toList());

        for (OrderRows orderRow : order.getOrderRows()) {
            BigDecimal taxValue = orderRow.getSumValues().getTotalDiscountedGross()
                    .subtract(orderRow.getSumValues().getTotalDiscountedNet());
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

    @Transactional
    public void enrichSalesOrder(SalesOrder salesOrder, Order order, Order originalOrder) {
        updatePaymentsAndOrderGroupId(salesOrder, order);
        salesOrder.setOriginalOrder(originalOrder);
        salesOrder.setLatestJson(order);
        salesOrder.setOrderGroupId(salesOrder.getOrderNumber());
        mapCustomerEmailIfExists(salesOrder, order);
    }

    private static void mapCustomerEmailIfExists(SalesOrder salesOrder, Order order) {
        Optional.ofNullable(order.getOrderHeader().getCustomer().getCustomerEmail())
                .filter(StringUtils::isNoneBlank)
                .ifPresent(salesOrder::setCustomerEmail);
    }

    private static void updatePaymentsAndOrderGroupId(SalesOrder salesOrder, Order order) {
        var orderNumber = salesOrder.getOrderNumber();
        var targetPaymentList = order.getOrderHeader().getPayments();
        var sourcePaymentList = salesOrder.getLatestJson().getOrderHeader().getPayments();

        var updatedPayments = targetPaymentList.stream()
                .map(payment -> filterAndUpdatePayment(sourcePaymentList, payment, orderNumber))
                .collect(toList());
        order.getOrderHeader().setPayments(updatedPayments);
        order.getOrderHeader().setOrderGroupId(orderNumber);
    }

    private static Payments filterAndUpdatePayment(List<Payments> sourcePaymentsList, Payments target, String orderNumber) {

        if (sourcePaymentsList.get(0).getType().equals("stored_creditcard") & target.getType().equals("creditcard"))
        {
            target.setType("stored_creditcard");
        }

        if (sourcePaymentsList.get(0).getType().equals("business_to_business_invoice") & target.getType().equals("b2b_cash_on_delivery"))
        {
            sourcePaymentsList.get(0).setType("b2b_cash_on_delivery");
        }

        if (target.getType().equals("cc_filiale")) {
            switch(sourcePaymentsList.get(0).getType()) {
                case "self_pickup":
                case "cash":
                case "ec_cash":
                    sourcePaymentsList.get(0).setType("cc_filiale");
                    break;
                default:
                    break;
            }
        }

        if (target.getType().equals("ec_cash")) {
            if ("self_pickup".equals(sourcePaymentsList.get(0).getType())) {
                sourcePaymentsList.get(0).setType("ec_cash");
            }
            sourcePaymentsList.get(0).setType("ec_cash");
        }

        if (target.getType().equals("payment_in_advance")) {
            switch (sourcePaymentsList.get(0).getType()) {
                case "creditcard":
                case "stored_creditcard":
                case "sofortuberweisung":
                    sourcePaymentsList.get(0).setType("payment_in_advance");
                    break;
                case "paypal":
                    target.setType("paypal");
                    break;
                default:
                    break;
            }
        }

        if (target.getType().equals("none")) {
            target.setType(sourcePaymentsList.get(0).getType());
        }




        return sourcePaymentsList.stream().filter(payment -> payment.getType().equals(target.getType()))
                .map(payment -> updatePaymentProvider(payment, target))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Order does not contain a valid payment type. Order number: " +
                                orderNumber));
    }

    public void enrichInitialOrder(Order initialOrder) {
        setOrderGroupIdIfEmpty(initialOrder);
        updateCustomSegment(initialOrder);
    }

    public void updateCustomSegment(Order order) {
        var customer = order.getOrderHeader().getCustomer();
        Predicate<String> customSegmentPredicate = cs -> equalsIgnoreCase(cs, B2B.getName())
                || equalsIgnoreCase(cs, DIRECT_DELIVERY.getName());

        if (Objects.isNull(customer.getCustomerSegment()) ||
                customer.getCustomerSegment().stream().noneMatch(customSegmentPredicate)) {
            var customSegmentUpdate = new ArrayList<CustomerSegment>();

            if (customer.getCustomerType() == BUSINESS) {
                customSegmentUpdate.add(B2B);
            }

            if (order.getOrderRows().stream()
                    .anyMatch(orderRow ->
                            equalsIgnoreCase(orderRow.getShippingType(), "direct delivery"))) {
                customSegmentUpdate.add(DIRECT_DELIVERY);
            }

            var customSegmentUpdateNames = customSegmentUpdate.stream()
                    .map(CustomerSegment::getName)
                    .collect(toList());

            if (!customSegmentUpdateNames.isEmpty()) {
                if (Objects.isNull(customer.getCustomerSegment())) {
                    customer.setCustomerSegment(new ArrayList<>());
                }
                customer.getCustomerSegment().addAll(customSegmentUpdateNames);
            }
        }
    }

    private void setOrderGroupIdIfEmpty(Order order) {
        String orderNumber = order.getOrderHeader().getOrderNumber();
        if (StringUtils.isBlank(order.getOrderHeader().getOrderGroupId())) {
            order.getOrderHeader().setOrderGroupId(orderNumber);
        }
    }

    public int getNextOrderNumberIndexCounter(String orderGroupId) {
        List<String> orderNumberList = orderRepository.findOrderNumberByOrderGroupId(orderGroupId);
        if (orderNumberList == null || orderNumberList.isEmpty()) {
            throw new SalesOrderNotFoundCustomException(
                    format("for the given order group id {0}", orderGroupId));
        }
        String separator = ORDER_NUMBER_SEPARATOR;

        Integer max = orderNumberList.stream()
                .filter(number -> number.contains(separator))
                .map(number -> Integer.valueOf(number.substring(number.lastIndexOf(separator) + 1)))
                .reduce(0, Integer::max);
        return max + 1;
    }
}
