package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNote;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNoteHeader;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.salesOrderHub.services.financialdocuments.CreditNoteNumberCounterService;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.CREDIT_NOTE_NUMBER_SEPARATOR;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.DATE_TIME_FORMATTER;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderReturnService {

    @NonNull
    private final SalesOrderReturnRepository salesOrderReturnRepository;

    @NonNull
    private final AuditLogRepository auditLogRepository;

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final CreditNoteNumberCounterService creditNoteNumberCounterService;

    @NonNull
    private final CamundaHelper helper;

    @NonNull
    private final OrderUtil orderUtil;

    public Optional<SalesOrderReturn> getByOrderNumber(String orderNumber) {
        return salesOrderReturnRepository.findByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    public Optional<SalesOrderReturn> getReturnOrder(String orderNumber, String creditNoteNumber) {

        var oldReturnOrderNumber = orderUtil.createOldFormatReturnOrderNumberInSOH(orderNumber, creditNoteNumber);
        var newReturnOrderNumber = orderUtil.createReturnOrderNumberInSOH(creditNoteNumber);

        Optional<SalesOrderReturn> returnOrderWithNewPattern = getByOrderNumber(newReturnOrderNumber);
        if (returnOrderWithNewPattern.isPresent()) {
            return returnOrderWithNewPattern;
        }

        return getByOrderNumber(oldReturnOrderNumber);
    }

    @Transactional(readOnly = true)
    public boolean checkReturnOrderNotExists(String orderNumber, final String creditNoteNumber) {
        if (getReturnOrder(orderNumber, creditNoteNumber).isPresent()) {
            log.warn("The following order return won't be processed because it exists in SOH system already from another source. " +
                    "Order Number: {} and Credit Note Number: {}", orderNumber, creditNoteNumber);
            return false;
        }
        return true;
    }

    public void updateUrl(String orderNumber, String url) {
        salesOrderReturnRepository.updateUrl(orderNumber, url);
    }

    @Transactional
    public SalesOrderReturn save(SalesOrderReturn order, Action action) {
        final var storedOrder = salesOrderReturnRepository.save(order);

        final var auditLog = AuditLog.builder()
                .salesOrderId(storedOrder.getId())
                .action(action)
                .data(order.getReturnOrderJson())
                .build();

        auditLogRepository.save(auditLog);
        return storedOrder;
    }

    public String createCreditNoteNumber() {
        var currentYear = LocalDateTime.now().getYear();
        return createCreditNoteNumber(currentYear);
    }

    String createCreditNoteNumber(int currentYear) {
        return currentYear + CREDIT_NOTE_NUMBER_SEPARATOR + getNextCreditNoteCount(currentYear);
    }

    private String getNextCreditNoteCount(int currentYear) {
        Long creditNoteNumber = creditNoteNumberCounterService.getNextCounter(currentYear);
        return String.format("%05d", creditNoteNumber);
    }

    @Transactional
    public void handleSalesOrderReturn(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage, Action action, Messages message) {
        var salesCreditNoteHeader = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader();
        var orderNumber = salesCreditNoteHeader.getOrderNumber();
        var creditNoteNumber = salesCreditNoteHeader.getCreditNoteNumber();

        if (checkReturnOrderNotExists(salesCreditNoteHeader.getOrderGroupId(), creditNoteNumber)) {
            var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                    .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

            SalesOrderReturn salesOrderReturn = createSalesOrderReturn(
                    salesCreditNoteCreatedMessage,
                    salesOrder);

            ProcessInstance result = helper.createReturnOrderProcess(save(salesOrderReturn, action), message);
            if (result != null) {
                log.info("New return order process started for order number: {}. Process-Instance-ID: {} ",
                        orderNumber, result.getProcessInstanceId());
            }
        }
    }

    private SalesOrderReturn createSalesOrderReturn(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage,
                                                    SalesOrder salesOrder) {
        var creditNoteNumber =
                salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber();
        var newReturnOrderNumber = orderUtil.createReturnOrderNumberInSOH(creditNoteNumber);

        return SalesOrderReturn.builder()
                .orderGroupId(salesOrder.getOrderGroupId())
                .orderNumber(newReturnOrderNumber)
                .returnOrderJson(
                        createReturnOrderJson(salesCreditNoteCreatedMessage, salesOrder, newReturnOrderNumber))
                .salesOrder(salesOrder)
                .salesCreditNoteCreatedMessage(
                        createCreditNoteEventMessage(salesOrder, salesCreditNoteCreatedMessage, newReturnOrderNumber))
                .build();
    }

    private Order createReturnOrderJson(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage,
                                        SalesOrder salesOrder, String newReturnOrderNumber) {
        var creditNoteLines =
                salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteLines();
        var returnOrderJson = recalculateOrderByReturns(salesOrder, getOrderRowUpdateItems(creditNoteLines));

        updateShippingCosts(returnOrderJson, creditNoteLines);
        returnOrderJson = orderUtil.removeInvalidGrandTotalTaxes(returnOrderJson);
        returnOrderJson.getOrderHeader().setOrderNumber(newReturnOrderNumber);
        returnOrderJson.getOrderHeader().setOrderGroupId(salesOrder.getOrderGroupId());
        returnOrderJson.getOrderHeader().setOrderDateTime(DATE_TIME_FORMATTER.format(LocalDateTime.now()));
        return returnOrderJson;
    }

    public Order recalculateOrderByReturns(SalesOrder salesOrder, Collection<CreditNoteLine> items) {

        var returnLatestJson = orderUtil.copyOrderJson(salesOrder.getLatestJson());
        returnLatestJson.setOrderRows(new ArrayList<>());
        var totals = returnLatestJson.getOrderHeader().getTotals();
        var lastRowKey = orderUtil.getLastRowKey(salesOrder);

        for (CreditNoteLine item : items) {
            var orderRow = orderUtil.createNewOrderRow(item, salesOrder, lastRowKey);
            returnLatestJson.getOrderRows().add(orderRow);
            lastRowKey = orderUtil.updateLastRowKey(salesOrder, item.getItemNumber(), lastRowKey);
        }

        totals.setShippingCostGross(BigDecimal.ZERO);
        totals.setShippingCostNet(BigDecimal.ZERO);
        totals.setGoodsTotalGross(BigDecimal.ZERO);
        totals.setGoodsTotalNet(BigDecimal.ZERO);
        totals.setTotalDiscountGross(BigDecimal.ZERO);
        totals.setTotalDiscountNet(BigDecimal.ZERO);
        totals.setGrandTotalGross(BigDecimal.ZERO);
        totals.setGrandTotalNet(BigDecimal.ZERO);
        totals.setPaymentTotal(BigDecimal.ZERO);

        returnLatestJson.getOrderRows().stream()
                .map(OrderRows::getSumValues)
                .forEach(sumValues -> {
                    totals.setGoodsTotalGross(totals.getGoodsTotalGross().add(
                            Optional.ofNullable(sumValues.getGoodsValueGross()).orElse(BigDecimal.ZERO)));
                    totals.setGoodsTotalNet(totals.getGoodsTotalNet().add(
                            Optional.ofNullable(sumValues.getGoodsValueNet()).orElse(BigDecimal.ZERO)));
                    totals.setTotalDiscountGross(totals.getTotalDiscountGross().add(
                            Optional.ofNullable(sumValues.getDiscountGross()).orElse(BigDecimal.ZERO)));
                    totals.setTotalDiscountNet(totals.getTotalDiscountNet().add(
                            Optional.ofNullable(sumValues.getDiscountNet()).orElse(BigDecimal.ZERO)));
                });

        totals.setGrandTotalGross(totals.getGoodsTotalGross().subtract(
                Optional.ofNullable(totals.getTotalDiscountGross()).orElse(BigDecimal.ZERO)));
        totals.setGrandTotalNet(totals.getGoodsTotalNet().subtract(
                Optional.ofNullable(totals.getTotalDiscountNet()).orElse(BigDecimal.ZERO)));
        totals.setPaymentTotal(totals.getGrandTotalGross());
        totals.setGrandTotalTaxes(salesOrderService.calculateGrandTotalTaxes(returnLatestJson));

        returnLatestJson.getOrderHeader().setTotals(totals);
        return returnLatestJson;
    }

    private List<CreditNoteLine> getOrderRowUpdateItems(Collection<CreditNoteLine> negativedCreditNoteLine) {
        return negativedCreditNoteLine.stream()
                .filter(creditNoteLine -> !creditNoteLine.getIsShippingCost())
                .collect(Collectors.toList());
    }

    private void updateShippingCosts(Order returnOrder, Collection<CreditNoteLine> creditNoteLines) {
        var totals = returnOrder.getOrderHeader().getTotals();
        creditNoteLines.stream()
                .filter(CreditNoteLine::getIsShippingCost)
                .findFirst()
                .ifPresent(creditNoteLine -> {
                    totals.setShippingCostNet(creditNoteLine.getLineNetAmount());
                    totals.setShippingCostGross(creditNoteLine.getLineGrossAmount());
                    totals.setGrandTotalNet(totals.getGrandTotalNet().add(totals.getShippingCostNet()));
                    totals.setGrandTotalGross(totals.getGrandTotalGross().add(totals.getShippingCostGross()));
                    totals.setPaymentTotal(totals.getGrandTotalGross());
                    BigDecimal fullTaxValue = totals.getGrandTotalGross().subtract(totals.getGrandTotalNet());
                    BigDecimal sumTaxValues = totals.getGrandTotalTaxes().stream()
                            .map(GrandTotalTaxes::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal taxValueToAdd = fullTaxValue.subtract(sumTaxValues);
                    totals.getGrandTotalTaxes().stream().findFirst().
                            ifPresent(tax -> tax.setValue(tax.getValue().add(taxValueToAdd)));
                });
    }
    SalesCreditNoteCreatedMessage createCreditNoteEventMessage(SalesOrder salesOrder,
                                                                       SalesCreditNoteCreatedMessage message,
                                                                       String newOrderNumber) {
        return SalesCreditNoteCreatedMessage.builder()
                .salesCreditNote(SalesCreditNote.builder()
                        .salesCreditNoteHeader(SalesCreditNoteHeader.builder()
                                .orderNumber(newOrderNumber)
                                .orderGroupId(salesOrder.getOrderGroupId())
                                .creditNoteNumber(message.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber())
                                .creditNoteDate(message.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteDate())
                                .currencyCode(message.getSalesCreditNote().getSalesCreditNoteHeader().getCurrencyCode())
                                .netAmount(message.getSalesCreditNote().getSalesCreditNoteHeader().getNetAmount())
                                .grossAmount(message.getSalesCreditNote().getSalesCreditNoteHeader().getGrossAmount())
                                .billingAddress(message.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress())
                                .creditNoteLines(message.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteLines())
                                .build())
                        .deliveryNotes(message.getSalesCreditNote().getDeliveryNotes())
                        .build())
                .build();
    }
}
