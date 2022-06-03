package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.PersistentProperties;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Signals;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.property.KeyValueProperty;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNote;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNoteHeader;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderPackageItemLine;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.CurrencyType.convert;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_BOOKED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ITEM_SHIPPED;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getGrossValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getMultipliedValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.round;
import static java.text.MessageFormat.format;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toUnmodifiableSet;

@Service
@Slf4j
@RequiredArgsConstructor
public class DropshipmentOrderService {

    private final CamundaHelper helper;
    private final SalesOrderService salesOrderService;
    private final InvoiceService invoiceService;
    private final KeyValuePropertyService keyValuePropertyService;
    private final SalesOrderReturnService salesOrderReturnService;
    private final SalesOrderRowService salesOrderRowService;
    private final SnsPublishService snsPublishService;

    public void handleDropShipmentOrderConfirmed(DropshipmentPurchaseOrderBookedMessage message) {
        String orderNumber = message.getSalesOrderNumber();
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        salesOrder.getLatestJson().getOrderHeader().setOrderNumberExternal(message.getExternalOrderNumber());
        salesOrder = salesOrderService.save(salesOrder, DROPSHIPMENT_PURCHASE_ORDER_BOOKED);
        var isDropshipmentOrderBooked = message.getBooked();

        helper.correlateMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED, salesOrder,
                Variables.putValue(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), isDropshipmentOrderBooked));
    }

    public void handleDropshipmentPurchaseOrderReturnConfirmed(DropshipmentPurchaseOrderReturnConfirmedMessage message) {
        var salesCreditNoteCreatedMessage = buildSalesCreditNoteCreatedMessage(message);
        handleDropshipmentPurchaseOrderReturnConfirmed(message, salesCreditNoteCreatedMessage);
    }

    void handleDropshipmentPurchaseOrderReturnConfirmed(DropshipmentPurchaseOrderReturnConfirmedMessage message, SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage) {
        salesOrderRowService.handleSalesOrderReturn(salesCreditNoteCreatedMessage, DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED);
    }

    SalesCreditNoteCreatedMessage buildSalesCreditNoteCreatedMessage(DropshipmentPurchaseOrderReturnConfirmedMessage message) {
        String orderNumber = message.getSalesOrderNumber();
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        List<CreditNoteLine> creditNoteLines = new ArrayList<>();
        for (OrderRows orderRow: salesOrder.getLatestJson().getOrderRows()) {
            for (DropshipmentPurchaseOrderPackageItemLine item: message.getPackages().stream()
                    .flatMap(c -> c.getItems().stream()).collect(Collectors.toList())) {
                if (StringUtils.pathEquals(item.getProductNumber(), orderRow.getSku())) {
                    var quantity = Optional.ofNullable(BigDecimal.valueOf(item.getQuantity())).orElse(BigDecimal.ZERO);
                    var unitNetAmount = Optional.ofNullable(orderRow.getUnitValues().getDiscountedNet()).orElse(BigDecimal.ZERO);
                    var lineNetAmount = round(getMultipliedValue(unitNetAmount, quantity));
                    var taxRate = Optional.ofNullable(orderRow.getTaxRate()).orElse(BigDecimal.ZERO);
                    var unitGrossAmount = getGrossValue(unitNetAmount, taxRate);
                    var lineGrossAmount = round(getMultipliedValue(unitGrossAmount, quantity));
                    var lineTaxAmount = lineGrossAmount.subtract(lineNetAmount);
                    CreditNoteLine creditNoteLine = CreditNoteLine.builder()
                            .itemNumber(orderRow.getSku())
                            .taxRate(taxRate)
                            .quantity(quantity)
                            .unitNetAmount(unitNetAmount)
                            .lineNetAmount(lineNetAmount)
                            .lineTaxAmount(lineTaxAmount)
                            .isShippingCost(false)
                            .build();
                    creditNoteLines.add(creditNoteLine);
                }
            }
        }
        var creditNoteNumber = salesOrderReturnService.createCreditNoteNumber();
        var salesCreditNoteHeader = SalesCreditNoteHeader.builder()
                .creditNoteNumber(creditNoteNumber)
                .creditNoteDate(now())
                .currencyCode(convert(salesOrder.getLatestJson().getOrderHeader().getOrderCurrency()))
                .billingAddress(Address.fromBillingAddress(salesOrder.getLatestJson().getOrderHeader().getBillingAddress()))
                .creditNoteLines(creditNoteLines)
                .orderGroupId(salesOrder.getLatestJson().getOrderHeader().getOrderGroupId())
                .orderNumber(salesOrder.getLatestJson().getOrderHeader().getOrderNumber())
                .grossAmount(getSumValue(line -> line.getLineTaxAmount().add(line.getLineNetAmount()), creditNoteLines))
                .netAmount(getSumValue(line -> line.getLineNetAmount(), creditNoteLines))
                .build();
        var salesCreditNote = SalesCreditNote.builder()
                .deliveryNotes(new ArrayList<>())
                .salesCreditNoteHeader(salesCreditNoteHeader)
                .build();
        var salesCreditNoteCreatedMessage = SalesCreditNoteCreatedMessage.builder()
                .salesCreditNote(salesCreditNote)
                .build();

        return salesCreditNoteCreatedMessage;
    }

    public void handleDropShipmentOrderTrackingInformationReceived(DropshipmentShipmentConfirmedMessage message) {
        var orderNumber = message.getSalesOrderNumber();
        SalesOrder salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        var orderRows = salesOrder.getLatestJson().getOrderRows();
        var shippedItems = message.getItems();

        shippedItems.forEach(item ->
                orderRows.stream()
                        .filter(row -> StringUtils.pathEquals(row.getSku(), item.getProductNumber()))
                        .findFirst()
                        .ifPresentOrElse(row -> {
                            addParcelNumber(item, row);
                            addServiceProviderName(item, row);
                        }, () -> {
                            throw new NotFoundException(
                                    format("Could not find order row with SKU {0} for order {1}",
                                            item.getProductNumber(), orderNumber));
                        })
        );

        setDocumentRefNumber(salesOrder);
        salesOrder = salesOrderService.save(salesOrder, ORDER_ITEM_SHIPPED);

        var trackingLinks = shippedItems.stream()
                .map(ShipmentItem::getTrackingLink)
                .collect(toUnmodifiableSet());

        helper.correlateMessage(Messages.DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED, salesOrder,
                Variables.putValue(TRACKING_LINKS.getName(), trackingLinks));
    }

    @Transactional
    public void handleDropShipmentOrderRowCancellation(String orderNumber, String sku) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find dropshipment order: " + orderNumber));

        salesOrder.getLatestJson().getOrderRows().stream()
                .filter(orderRow -> StringUtils.pathEquals(orderRow.getSku(), sku))
                .findFirst()
                .ifPresentOrElse(orderRow -> {
                    orderRow.setIsCancelled(true);
                    salesOrderService.save(salesOrder, Action.ORDER_ROW_CANCELLED);
                    helper.correlateMessage(DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED, salesOrder,
                            Variables.putValue(ORDER_ROW_ID.getName(), orderRow.getSku()));
                }, () -> {
                    throw new NotFoundException(
                            format("Could not find order row with SKU {0} for order {1}",
                                    sku, orderNumber));
                });
    }

    public boolean isDropShipmentOrder(String orderNumber) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        var originalOrder = (Order) salesOrder.getOriginalOrder();
        var orderFulfillment = originalOrder.getOrderHeader().getOrderFulfillment();
        return org.apache.commons.lang3.StringUtils.equalsIgnoreCase(orderFulfillment, DELTICOM.getName());
    }

    public KeyValueProperty setPauseDropshipmentProcessing(Boolean newPauseDropshipmentProcessing) {
        var currentPauseDropshipmentProcessingProperty = keyValuePropertyService.getPropertyByKey(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING)
                .orElseThrow(() -> new NotFoundException("Could not found persistent property. Key:  " + PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING));

        var currentPauseDropshipmentProcessing = currentPauseDropshipmentProcessingProperty.getTypedValue();

        log.info("Current value of '{}' is '{}'", PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING,
                currentPauseDropshipmentProcessing);

        currentPauseDropshipmentProcessingProperty.setValue(newPauseDropshipmentProcessing.toString());

        var savedPauseDropshipmentProcessingProperty = keyValuePropertyService.save(currentPauseDropshipmentProcessingProperty);

        log.info("Set value of '{}' to '{}'", PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING, newPauseDropshipmentProcessing);

        if (Boolean.TRUE.equals(currentPauseDropshipmentProcessing) && Boolean.FALSE.equals(newPauseDropshipmentProcessing)) {
            continueProcessingDropShipmentOrder();
            log.info("Sent signal to all the process instances waiting for dropshipment order continuation");
        }

        return savedPauseDropshipmentProcessingProperty;
    }

    public void handleDropshipmentPurchaseOrderReturnNotified(
            MessageWrapper<DropshipmentPurchaseOrderReturnNotifiedMessage> messageWrapper) {

        var message = messageWrapper.getMessage();

        try {
            var salesOrder = salesOrderService.getOrderByOrderNumber(message.getSalesOrderNumber())
                    .orElseThrow(() -> new SalesOrderNotFoundException(message.getSalesOrderNumber()));

            snsPublishService.publishDropshipmentOrderReturnNotifiedEvent(salesOrder, message);
        } catch (Exception e) {
            log.error("Dropshipment purchase order return notified message error:\r\nOrderNumber: " +
                            "{}\r\nExternalOrderNumber: {}\r\nError-Message: {}",
                    message.getSalesOrderNumber(),
                    message.getExternalOrderNumber(),
                    e.getMessage());
            throw e;
        }
    }

    private void continueProcessingDropShipmentOrder() {
        helper.sendSignal(Signals.CONTINUE_PROCESSING_DROPSHIPMENT_ORDERS);
    }

    private void addParcelNumber(ShipmentItem item, OrderRows row) {
        var parcelNumber = item.getParcelNumber();
        Optional.ofNullable(row.getTrackingNumbers())
                .ifPresentOrElse(trackingNumbers -> trackingNumbers.add(parcelNumber),
                        () -> row.setTrackingNumbers(List.of(parcelNumber)));
    }

    private void addServiceProviderName(ShipmentItem item, OrderRows row) {
        row.setShippingProvider(item.getServiceProviderName());
    }

    private void setDocumentRefNumber(SalesOrder salesOrder) {
        salesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber(invoiceService.createInvoiceNumber());
    }
}
