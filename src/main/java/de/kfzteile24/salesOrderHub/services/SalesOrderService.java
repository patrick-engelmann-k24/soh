package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderMapper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Payments;
import de.kfzteile24.soh.order.dto.Platform;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Surcharges;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.MIGRATION_SALES_ORDER_RECEIVED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.isNotNullAndEqual;
import static de.kfzteile24.salesOrderHub.helper.PaymentUtil.updatePaymentProvider;
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

    @NonNull
    private final RuntimeService runtimeService;

    @Transactional
    public SalesOrder updateProcessInstanceId(String orderNumber, String processInstanceId) {
        Optional<SalesOrder> salesOrderOptional = getOrderByOrderNumber(orderNumber);
        if (salesOrderOptional.isPresent()) {
            SalesOrder salesOrder = salesOrderOptional.get();

            salesOrder.setProcessId(processInstanceId);
            return updateOrder(salesOrder);
        }
        throw new SalesOrderNotFoundException(format("for order {}", orderNumber));
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

        CoreSalesInvoiceHeader salesInvoiceHeader = salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        Order order = createOrderForSubsequentSalesOrder(salesInvoiceCreatedMessage, originalSalesOrder);
        order.getOrderHeader().setPlatform(Platform.SOH);
        order.getOrderHeader().setOrderNumber(newOrderNumber);
        order.getOrderHeader().setOrderGroupId(originalSalesOrder.getOrderGroupId());
        order.getOrderHeader().setDocumentRefNumber(salesInvoiceHeader.getInvoiceNumber());
        salesInvoiceHeader.setOrderNumber(newOrderNumber);
        salesInvoiceHeader.setOrderGroupId(order.getOrderHeader().getOrderGroupId());
        var shippingCostDocumentLine = salesInvoiceHeader.getInvoiceLines().stream()
                .filter(CoreSalesFinancialDocumentLine::getIsShippingCost).findFirst().orElse(null);
        recalculateTotals(order, shippingCostDocumentLine);

        var customerEmail = Strings.isNotEmpty(originalSalesOrder.getCustomerEmail()) ?
                originalSalesOrder.getCustomerEmail() :
                getCustomerEmailByOrderJson(order);
        var salesOrder = SalesOrder.builder()
                .orderNumber(newOrderNumber)
                .orderGroupId(originalSalesOrder.getOrderGroupId())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .customerEmail(customerEmail)
                .originalOrder(order)
                .latestJson(order)
                .invoiceEvent(salesInvoiceCreatedMessage)
                .build();
        return createSalesOrder(salesOrder);
    }

    private String getCustomerEmailByOrderJson(Order order) {
        if (order.getOrderHeader().getCustomer() != null) {
            return order.getOrderHeader().getCustomer().getCustomerEmail();
        }
        throw new NotFoundException("Customer Email is not found for the order number: " +
                order.getOrderHeader().getOrderNumber());
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

        var shippingCostDocumentLine = items.stream().filter(item -> item.getIsShippingCost()).findFirst().orElse(null);

        return isShippingCostNetMatch(originalSalesOrder, shippingCostDocumentLine)
                && isShippingCostGrossMatch(originalSalesOrder, shippingCostDocumentLine);
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
        BigDecimal shippingCostNet = shippingCostLine != null ? shippingCostLine.getUnitNetAmount() : BigDecimal.ZERO;
        BigDecimal shippingCostGross = shippingCostLine != null ? shippingCostLine.getUnitGrossAmount() : BigDecimal.ZERO;
        recalculateTotals(order, shippingCostNet, shippingCostGross, shippingCostLine != null);
    }

    public void recalculateTotals(Order order, BigDecimal shippingCostNet, BigDecimal shippingCostGross, boolean shippingCostLine) {

        List<OrderRows> orderRows = order.getOrderRows();
        List<SumValues> sumValues = orderRows.stream().map(OrderRows::getSumValues).collect(Collectors.toList());
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
                .map(this::createGrandTotalTaxesFromOrderRow).distinct().collect(Collectors.toList());

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

    public void handleInvoiceFromCore(String invoiceUrl) {
        final var orderNumber = InvoiceUrlExtractor.extractOrderNumber(invoiceUrl);

        log.info("Received invoice from core with order number: {} ", orderNumber);

        final Map<String, Object> processVariables = Map.of(
                ORDER_NUMBER.getName(), orderNumber,
                INVOICE_URL.getName(), invoiceUrl
        );
        runtimeService.startProcessInstanceByMessage(Messages.INVOICE_CREATED.getName(), orderNumber, processVariables);
        log.info("Invoice {} from core for order-number {} successfully received", invoiceUrl, orderNumber);
    }

    public void handleCreditNoteFromDropshipmentOrderReturn(String invoiceUrl) {
        final var returnOrderNumber = InvoiceUrlExtractor.extractOrderNumber(invoiceUrl);

        log.info("Received credit note from dropshipment with return order number: {} ", returnOrderNumber);

        final Map<String, Object> processVariables = Map.of(
                ORDER_NUMBER.getName(), returnOrderNumber,
                INVOICE_URL.getName(), invoiceUrl
        );

        runtimeService.startProcessInstanceByMessage(Messages.DROPSHIPMENT_CREDIT_NOTE_CREATED.getName(), returnOrderNumber, processVariables);
        log.info("Invoice {} for credit note of dropshipment order return for order-number {} successfully received", invoiceUrl, returnOrderNumber);
    }

    @Transactional
    public void updateSalesOrderByOrderJson(SalesOrder salesOrder, Order order) {
        updatePaymentsAndOrderGroupId(salesOrder, order);
        salesOrder.setOriginalOrder(order);
        salesOrder.setLatestJson(order);
        salesOrder.setOrderGroupId(salesOrder.getOrderNumber());
        mapCustomerEmailIfExists(salesOrder, order);
        save(salesOrder, MIGRATION_SALES_ORDER_RECEIVED);
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
                .collect(Collectors.toList());
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
            switch (sourcePaymentsList.get(0).getType()) {
                case "self_pickup":
                    sourcePaymentsList.get(0).setType("ec_cash");
                    break;
                case "cash":
                    sourcePaymentsList.get(0).setType("ec_cash");
                    break;
                default:
                    break;
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
}
