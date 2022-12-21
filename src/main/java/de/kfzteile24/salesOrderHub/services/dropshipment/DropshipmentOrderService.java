package de.kfzteile24.salesOrderHub.services.dropshipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.CustomEventName;
import de.kfzteile24.salesOrderHub.constants.PersistentProperties;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Signals;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.property.KeyValueProperty;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.ReturnOrderHelper;
import de.kfzteile24.salesOrderHub.helper.SubsequentSalesOrderCreationHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_ORDER_RETURN_CREATED;
import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_ORDER_RETURN_NOTIFIED;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.ORDER_NUMBER_SEPARATOR;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_INVOICE_STORED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_BOOKED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ITEM_SHIPPED;
import static de.kfzteile24.salesOrderHub.helper.SubsequentSalesOrderCreationHelper.buildSubsequentSalesOrder;
import static java.text.MessageFormat.format;
import static java.util.function.Predicate.not;

@Service
@Slf4j
@RequiredArgsConstructor
public class DropshipmentOrderService {

    private final CamundaHelper helper;
    private final SalesOrderService salesOrderService;
    private final SalesOrderRowService salesOrderRowService;
    private final InvoiceService invoiceService;
    private final KeyValuePropertyService keyValuePropertyService;
    private final SalesOrderReturnService salesOrderReturnService;
    private final SnsPublishService snsPublishService;
    private final ReturnOrderHelper returnOrderHelper;
    private final ObjectMapper objectMapper;
    private final CamundaHelper camundaHelper;
    private final OrderUtil orderUtil;

    @NotNull
    private final SubsequentSalesOrderCreationHelper subsequentOrderHelper;
    @NotNull
    private final MetricsHelper metricsHelper;

    @EnrichMessageForDlq
    public void handleDropShipmentOrderConfirmed(
            DropshipmentPurchaseOrderBookedMessage message, MessageWrapper messageWrapper) {

        String orderNumber = message.getSalesOrderNumber();
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        salesOrder.getLatestJson().getOrderHeader().setOrderNumberExternal(message.getExternalOrderNumber());
        salesOrder = salesOrderService.save(salesOrder, DROPSHIPMENT_PURCHASE_ORDER_BOOKED);
        var isDropshipmentOrderBooked = message.getBooked();

        helper.correlateMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED, salesOrder,
                Variables.putValue(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), isDropshipmentOrderBooked));
        if (isDropshipmentOrderBooked) {
            metricsHelper.sendCustomEventForDropshipmentOrder(salesOrder, CustomEventName.DROPSHIPMENT_ORDER_CONFIRMED);
        } else {
            metricsHelper.sendCustomEventForDropshipmentOrder(salesOrder, DROPSHIPMENT_ORDER_CANCELLED);
        }
    }

    @EnrichMessageForDlq
    public void handleDropshipmentPurchaseOrderReturnConfirmed(
            DropshipmentPurchaseOrderReturnConfirmedMessage message, MessageWrapper messageWrapper) {
        checkDropshipmentOrderReturnIsPaused(message);
        var salesCreditNoteCreatedMessage = buildSalesCreditNoteCreatedMessage(message);

        var orderNumber = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        log.info("Received dropshipment purchase order return confirmed message with order number: {}", orderNumber);

        salesOrderReturnService.handleSalesOrderReturn(salesCreditNoteCreatedMessage, DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED, Messages.DROPSHIPMENT_ORDER_RETURN_CONFIRMED);
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        metricsHelper.sendCustomEventForDropshipmentOrder(salesOrder, DROPSHIPMENT_ORDER_RETURN_CREATED);
    }

    public SalesCreditNoteCreatedMessage buildSalesCreditNoteCreatedMessage(DropshipmentPurchaseOrderReturnConfirmedMessage message) {
        var orderNumber = message.getSalesOrderNumber();
        SalesOrder salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        String creditNoteNumber = salesOrderReturnService.createCreditNoteNumber();
        return returnOrderHelper.buildSalesCreditNoteCreatedMessage(message, salesOrder, creditNoteNumber);
    }

    @EnrichMessageForDlq
    public void handleDropShipmentOrderTrackingInformationReceived(
            DropshipmentShipmentConfirmedMessage message, MessageWrapper messageWrapper) {

        final var orderNumber = message.getSalesOrderNumber();
        SalesOrder salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        final var shippedItems = message.getItems();
        final var orderRows = salesOrder.getLatestJson().getOrderRows();

        final var savedSalesOrder = updateSalesOrderWithTrackingInformation(
                salesOrder, shippedItems, orderRows);

        sendShipmentConfirmedMessageForEachOrderRow(savedSalesOrder, shippedItems, orderRows);
    }

    private SalesOrder updateSalesOrderWithTrackingInformation(SalesOrder salesOrder,
                                                               Collection<ShipmentItem> shippedItems,
                                                               List<OrderRows> orderRows) {
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
                                            item.getProductNumber(), salesOrder.getOrderNumber()));
                        })
        );
        return salesOrderService.save(salesOrder, ORDER_ITEM_SHIPPED);
    }

    private void sendShipmentConfirmedMessageForEachOrderRow(SalesOrder savedSalesOrder,
                                                             Collection<ShipmentItem> shippedItems,
                                                             List<OrderRows> orderRows) {
        final var skuMap = getSkuMap(shippedItems);
        shippedItems.forEach(item ->
                orderRows.stream()
                        .filter(row -> StringUtils.pathEquals(row.getSku(), item.getProductNumber()))
                        .forEach(row ->
                                camundaHelper.correlateDropshipmentOrderRowShipmentConfirmedMessage(savedSalesOrder, row.getSku(),
                                        Collections.singletonList(getTrackingLink(item, skuMap)))
                        )
        );
    }

    /*
        This method groups the sku names according to tracking link information if the tracking link is the same for multiple sku
     */
    private Map<String, List<String>> getSkuMap(Collection<ShipmentItem> shippedItems) {
        Map<String, List<String>> skuMap = new HashMap<>();
        shippedItems.forEach(item -> {
            var key = item.getTrackingLink();
            var value = item.getProductNumber();
            var valueList = skuMap.get(key);
            if (valueList == null) {
                valueList = new ArrayList<>(List.of(value));
            } else {
                valueList.add(value);
            }
            skuMap.put(key, valueList);
        });
        return skuMap;
    }

    @SneakyThrows
    private String getTrackingLink(ShipmentItem shipmentItem, Map<String, List<String>> skuMap) {
        return objectMapper.writeValueAsString(TrackingLink.builder()
                .url(shipmentItem.getTrackingLink())
                .orderItems(skuMap.get(shipmentItem.getTrackingLink()))
                .build());
    }

    @Transactional
    public void handleDropShipmentOrderRowCancellation(String orderNumber, String sku) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find dropshipment order: " + orderNumber));

        salesOrder.getLatestJson().getOrderRows().stream()
                .filter(not(OrderRows::getIsCancelled))
                .filter(orderRow -> StringUtils.pathEquals(orderRow.getSku(), sku))
                .findFirst()
                .ifPresentOrElse(orderRow -> {
                    orderRow.setIsCancelled(true);
                    salesOrderService.save(salesOrder, Action.ORDER_ROW_CANCELLED);
                }, () -> {
                    throw new NotFoundException(
                            format("Could not find order row with SKU {0} for order {1}",
                                    sku, orderNumber));
                });
    }

    @Transactional
    public void handleDropShipmentOrderCancellation(String orderNumber, String processInstanceId) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find dropshipment order: " + orderNumber));

        camundaHelper.setVariable(processInstanceId,
                IS_ORDER_CANCELLED.getName(),
                salesOrderRowService.cancelOrderProcessIfFullyCancelled(salesOrder));
    }

    public boolean isDropShipmentOrder(String orderNumber) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        var latestOrder = salesOrder.getLatestJson();
        var orderFulfillment = latestOrder.getOrderHeader().getOrderFulfillment();
        return org.apache.commons.lang3.StringUtils.equalsIgnoreCase(orderFulfillment, DELTICOM.getName());
    }

    private KeyValueProperty setValueOfKeyValueProperty(String key, Boolean newValue) {
        var currentKeyValueProperty = keyValuePropertyService.getPropertyByKey(key)
                .orElseThrow(() -> new NotFoundException("Could not found persistent property. Key:  " + key));

        var currentKeyValueObject = currentKeyValueProperty.getTypedValue();

        log.info("Current value of '{}' is '{}'", key, currentKeyValueObject);

        currentKeyValueProperty.setValue(newValue.toString());

        var savedKeyValueProperty = keyValuePropertyService.save(currentKeyValueProperty);

        log.info("Set value of '{}' to '{}'", key, newValue);

        return savedKeyValueProperty;
    }

    private void checkDropshipmentOrderReturnIsPaused(DropshipmentPurchaseOrderReturnConfirmedMessage message) {
        var preventDropshipmentOrderReturnConfirmed =
                keyValuePropertyService.getPropertyByKey(PersistentProperties.PREVENT_DROPSHIPMENT_ORDER_RETURN_CONFIRMED)
                        .orElseThrow(() -> {
                            throw new NotFoundException("Could not find persistent property with key " +
                                    "'preventDropshipmentOrderReturnConfirmed'");
                        });

        var orderNumber = message.getSalesOrderNumber();

        if (Boolean.TRUE.equals(preventDropshipmentOrderReturnConfirmed.getTypedValue())) {
            var errorMsg = MessageFormat.format(
                    "Dropshipment Order Return Confirmed process is inactive. " +
                            "Message with Order number {0} is moved to DLQ", orderNumber);
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        } else {
            log.info("Received dropshipment purchase order return confirmed message with Sales Order Number: {}, " +
                    "External Order NUmber: {}", orderNumber, message.getExternalOrderNumber());
        }
    }

    public KeyValueProperty setPauseDropshipmentProcessing(Boolean newPauseDropshipmentProcessing) {

        var currentKeyValueProperty = keyValuePropertyService.getPropertyByKey(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING)
                .orElseThrow(() -> new NotFoundException("Could not found persistent property. Key:  " + PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING));

        var savedPauseDropshipmentProcessingProperty =
                setValueOfKeyValueProperty(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING,
                        newPauseDropshipmentProcessing);

        continueProcessingDropShipmentOrder(currentKeyValueProperty, newPauseDropshipmentProcessing);

        return savedPauseDropshipmentProcessingProperty;
    }

    public KeyValueProperty setPreventDropshipmentOrderReturnConfirmed(Boolean newPreventDropshipmentOrderReturnConfirmed) {
        return setValueOfKeyValueProperty(PersistentProperties.PREVENT_DROPSHIPMENT_ORDER_RETURN_CONFIRMED,
                newPreventDropshipmentOrderReturnConfirmed);
    }

    @EnrichMessageForDlq
    public void handleDropshipmentPurchaseOrderReturnNotified(
            DropshipmentPurchaseOrderReturnNotifiedMessage message, MessageWrapper messageWrapper) {

        try {
            var salesOrder = salesOrderService.getOrderByOrderNumber(message.getSalesOrderNumber())
                    .orElseThrow(() -> new SalesOrderNotFoundException(message.getSalesOrderNumber()));

            snsPublishService.publishDropshipmentOrderReturnNotifiedEvent(salesOrder, message);
            metricsHelper.sendCustomEventForDropshipmentOrder(salesOrder, DROPSHIPMENT_ORDER_RETURN_NOTIFIED);
        } catch (Exception e) {
            log.error("Dropshipment purchase order return notified message error:\r\nOrderNumber: " +
                            "{}\r\nExternalOrderNumber: {}\r\nError-Message: {}",
                    message.getSalesOrderNumber(),
                    message.getExternalOrderNumber(),
                    e.getMessage());
            throw e;
        }
    }

    @Transactional
    public SalesOrder recreateSalesOrderInvoice(String orderNumber) {
        return salesOrderService.getOrderByOrderNumber(orderNumber)
                .map(salesOrder -> {
                    setDocumentRefNumber(salesOrder);
                    salesOrder.setInvoiceEvent(invoiceService.generateInvoiceMessage(salesOrder));
                    return salesOrderService.save(salesOrder, DROPSHIPMENT_INVOICE_STORED);
                })
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
    }

    private void continueProcessingDropShipmentOrder(KeyValueProperty currentPauseDropshipmentProcessingProperty, Boolean newPauseDropshipmentProcessing) {

        var currentPauseDropshipmentProcessing = currentPauseDropshipmentProcessingProperty.getTypedValue();

        if (Boolean.TRUE.equals(currentPauseDropshipmentProcessing) && Boolean.FALSE.equals(newPauseDropshipmentProcessing)) {
            helper.sendSignal(Signals.CONTINUE_PROCESSING_DROPSHIPMENT_ORDERS);
            log.info("Sent signal to all the process instances waiting for dropshipment order continuation");
        }
    }

    private void addParcelNumber(ShipmentItem item, OrderRows row) {
        var parcelNumber = item.getParcelNumber();
        Optional.ofNullable(row.getTrackingNumbers())
                .ifPresentOrElse(trackingNumbers -> trackingNumbers.add(parcelNumber),
                        () -> row.setTrackingNumbers(new ArrayList<>(Collections.singleton(parcelNumber))));
    }

    private void addServiceProviderName(ShipmentItem item, OrderRows row) {
        row.setShippingProvider(item.getServiceProviderName());
    }

    private void setDocumentRefNumber(SalesOrder salesOrder) {
        salesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber(invoiceService.createInvoiceNumber());
    }

    public void startDropshipmentSubsequentOrderProcess(SalesOrder subsequentOrder) {
        camundaHelper.startDropshipmentSubsequentOrderCreatedProcess(subsequentOrder);
    }

    public SalesOrder createDropshipmentSubsequentSalesOrder(SalesOrder salesOrder,
                                                             List<String> skuList,
                                                             String invoiceNumber,
                                                             String activityInstanceId) {
        String newOrderNumber = createDropshipmentNewOrderNumber(salesOrder);
        Order orderJson = createDropshipmentSubsequentOrderJson(salesOrder, newOrderNumber, skuList, invoiceNumber);
        var subsequentOrder = buildSubsequentSalesOrder(orderJson, newOrderNumber);
        subsequentOrder.setProcessId(activityInstanceId);

        return salesOrderService.save(subsequentOrder, ORDER_CREATED);
    }

    public Order createDropshipmentSubsequentOrderJson(SalesOrder salesOrder,
                                                       String newOrderNumber,
                                                       List<String> skuList,
                                                       String invoiceNumber) {
        Order orderJson = Order.builder()
                .version(salesOrder.getLatestJson().getVersion())
                .orderHeader(subsequentOrderHelper.createOrderHeader(salesOrder, newOrderNumber, invoiceNumber))
                .build();

        orderJson.setOrderRows(salesOrder.getLatestJson().getOrderRows().stream()
                .filter(row -> skuList.contains(row.getSku()))
                .collect(Collectors.toList()));

        salesOrderService.recalculateTotals(orderJson, invoiceService.getShippingCostLine(salesOrder));
        removeShippingCostFromOriginalOrder(salesOrder);
        return orderJson;
    }

    private void removeShippingCostFromOriginalOrder(SalesOrder salesOrder) {
        var totals = salesOrder.getLatestJson().getOrderHeader().getTotals();
        totals.setShippingCostGross(BigDecimal.ZERO);
        totals.setShippingCostNet(BigDecimal.ZERO);
        salesOrderService.save(salesOrder, DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED);
    }

    public String createDropshipmentNewOrderNumber(SalesOrder salesOrder) {
        int nextIndexCounter =
                salesOrderService.getNextOrderNumberIndexCounter(salesOrder.getOrderGroupId());
        return salesOrder.getOrderGroupId() + ORDER_NUMBER_SEPARATOR + nextIndexCounter;
    }
}
