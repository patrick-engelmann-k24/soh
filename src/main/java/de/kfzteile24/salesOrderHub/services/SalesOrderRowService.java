package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShipped;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Platform;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.COMBINED_ITEM_SEPARATOR;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderRowService {

    @NonNull
    private final CamundaHelper helper;

    @NonNull
    private final RuntimeService runtimeService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final SnsPublishService snsPublishService;

    public boolean cancelOrderProcessIfFullyCancelled(SalesOrder salesOrder) {
        if (salesOrder.getLatestJson().getOrderRows().stream().allMatch(OrderRows::getIsCancelled)) {
            log.info("Order with order number: {} is fully cancelled", salesOrder.getOrderNumber());
            for (OrderRows orderRow : salesOrder.getLatestJson().getOrderRows()) {
                if (!helper.isShipped(orderRow.getShippingType())) {
                    orderRow.setIsCancelled(true);
                }
            }
            salesOrder.setCancelled(true);
            salesOrderService.save(salesOrder, Action.ORDER_CANCELLED);
            return true;
        } else {
            return false;
        }
    }

    @Transactional
    public SalesOrder cancelOrder(String orderNumber) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        log.info("Order with order number: {} is being fully cancelled", salesOrder.getOrderNumber());
        salesOrder.setCancelled(true);
        return salesOrderService.save(salesOrder, Action.ORDER_CANCELLED);
    }

    public void cancelOrderRow(String orderRowId, String orderNumber) {

        markOrderRowAsCancelled(orderNumber, orderRowId);

        if (helper.checkIfActiveProcessExists(orderNumber)) {
            removeCancelledOrderRowFromProcessVariables(orderNumber, orderRowId);
        } else {
            log.debug("Sales order process does not exist for order number {}", orderNumber);
        }

        snsPublishService.publishOrderRowCancelled(orderNumber, orderRowId);
        log.debug("Published Order row cancelled for order number: {} and order row: {}", orderNumber, orderRowId);
        log.info("Order row cancelled for order number: {} and order row: {}", orderNumber, orderRowId);
    }

    @SuppressWarnings("unchecked")
    private void removeCancelledOrderRowFromProcessVariables(String orderNumber, String orderRowId) {
        var processInstance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_PROCESS.getName())
                .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .singleResult();

        var orderRows = (List<String>) runtimeService.getVariable(processInstance.getId(),
                Variables.ORDER_ROWS.getName());
        orderRows.remove(orderRowId);
        runtimeService.setVariable(processInstance.getId(), Variables.ORDER_ROWS.getName(), orderRows);
    }

    private void markOrderRowAsCancelled(String orderNumber, String orderRowId) {

        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));

        final var latestJson = salesOrder.getLatestJson();
        OrderRows cancelledOrderRow = latestJson.getOrderRows().stream()
                .filter(row -> !row.getIsCancelled())
                .filter(row -> orderRowId.equals(row.getSku())).findFirst()
                .orElseThrow(() -> new NotFoundException(
                        MessageFormat.format("Could not find order row with SKU {0} for order {1}",
                                orderRowId, orderNumber)));
        cancelledOrderRow.setIsCancelled(true);

        recalculateOrder(latestJson, cancelledOrderRow);
        salesOrderService.save(salesOrder, Action.ORDER_ROW_CANCELLED);

        boolean isOrderCancelled = cancelOrderProcessIfFullyCancelled(salesOrder);
        log.info("Is order with order number: {} fully check result: {}", orderNumber, isOrderCancelled);
        var processInstance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_PROCESS.getName())
                .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .singleResult();
        if (processInstance != null)
            runtimeService.setVariable(processInstance.getId(), Variables.IS_ORDER_CANCELLED.getName(), isOrderCancelled);
    }

    private void recalculateOrder(Order latestJson, OrderRows cancelledOrderRow) {

        SumValues sumValues = cancelledOrderRow.getSumValues();
        Totals totals = latestJson.getOrderHeader().getTotals();

        BigDecimal goodsTotalGross = totals.getGoodsTotalGross().subtract(sumValues.getGoodsValueGross());
        BigDecimal goodsTotalNet = totals.getGoodsTotalNet().subtract(sumValues.getGoodsValueNet());
        BigDecimal totalDiscountGross = Optional.ofNullable(totals.getTotalDiscountGross()).orElse(BigDecimal.ZERO)
                .subtract(Optional.ofNullable(sumValues.getDiscountGross()).orElse(BigDecimal.ZERO));
        BigDecimal totalDiscountNet = Optional.ofNullable(totals.getTotalDiscountNet()).orElse(BigDecimal.ZERO)
                .subtract(Optional.ofNullable(sumValues.getDiscountNet()).orElse(BigDecimal.ZERO));
        BigDecimal grandTotalGross = goodsTotalGross.subtract(totalDiscountGross);
        BigDecimal grantTotalNet = goodsTotalNet.subtract(totalDiscountNet);
        BigDecimal cancelledOrderRowTaxValue = sumValues.getGoodsValueGross().subtract(sumValues.getGoodsValueNet());
        totals.getGrandTotalTaxes().stream()
                .filter(tax -> tax.getRate().equals(cancelledOrderRow.getTaxRate())).findFirst()
                .ifPresent(tax -> tax.setValue(tax.getValue().subtract(cancelledOrderRowTaxValue)));

        totals.setGoodsTotalGross(goodsTotalGross);
        totals.setGoodsTotalNet(goodsTotalNet);
        totals.setTotalDiscountGross(totalDiscountGross);
        totals.setTotalDiscountNet(totalDiscountNet);
        totals.setGrandTotalGross(grandTotalGross);
        totals.setGrandTotalNet(grantTotalNet);
        totals.setPaymentTotal(grandTotalGross);
        latestJson.getOrderHeader().setTotals(totals);
    }

    private List<String> getOriginalOrderSkus(String orderNumber) {

        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        Order originalOrder = (Order) salesOrder.getOriginalOrder();
        return originalOrder.getOrderRows().stream().map(OrderRows::getSku).collect(Collectors.toList());
    }

    private boolean isCorePlatformOrder(List<SalesOrder> salesOrders) {
        return Platform.CORE == ((Order)salesOrders.get(0).getOriginalOrder()).getOrderHeader().getPlatform();
    }

    public void handleParcelShippedEvent(ParcelShipped event) {
        var orderNumber = event.getOrderNumber();
        if (hasAnyCombinedItem(event)) {
            log.info("Order: {} has combined items, so it would be ignored for ParcelShippedEvent Handling", orderNumber);
        } else {
            List<SalesOrder> salesOrders = getSalesOrdersByGroupId(event, orderNumber);
            if (isCorePlatformOrder(salesOrders)) {
                log.info("Order: {} is a CORE Platform Order, so it would be ignored for ParcelShippedEvent Handling", orderNumber);
            } else {
                var itemList = event.getArticleItemsDtos();
                if (itemList == null || itemList.isEmpty()) {
                    throw new IllegalArgumentException("The provided event does not contain order item");
                }

                try {
                    SalesOrder salesOrder = getSalesOrderIncludesOrderItems(event, salesOrders).orElseThrow(() ->
                            buildNotFoundException(event)
                    );

                    TrackingLink trackingLink = TrackingLink.builder()
                            .url(event.getTrackingLink())
                            .orderItems(itemList.stream().map(ArticleItemsDto::getNumber).collect(Collectors.toList()))
                            .build();

                    snsPublishService.publishSalesOrderShipmentConfirmedEvent(salesOrder, List.of(trackingLink));
                } catch (Exception e) {
                    log.error("Parcel shipped received message error - order_number: {}\r\nErrorMessage: {}", orderNumber, e);
                    throw e;
                }
            }
        }
    }

    private Optional<SalesOrder> getSalesOrderIncludesOrderItems(ParcelShipped event, List<SalesOrder> salesOrders) {
        for (SalesOrder salesOrder : salesOrders) {
            List<String> orderSkuList =
                    salesOrder.getLatestJson().getOrderRows().stream().map(OrderRows::getSku).collect(Collectors.toList());
            if (isAllIncludedInSkuList(event, orderSkuList)) {
                return Optional.of(salesOrder);
            }
        }
        return Optional.empty();
    }

    private NotFoundException buildNotFoundException(ParcelShipped event) {
        return new NotFoundException(
                MessageFormat.format(
                        "There is no sales order including all article number " +
                                "in the parcel shipped event. " +
                                "OrderNumber: {0}, DeliveryNoteNumber: {1}, articleItemsList: {2}",
                        event.getOrderNumber(),
                        event.getDeliveryNoteNumber(),
                        event.getArticleItemsDtos().stream()
                                .map(ArticleItemsDto::getNumber)
                                .collect(Collectors.toList())
                ));
    }

    private List<SalesOrder> getSalesOrdersByGroupId(ParcelShipped event, String orderNumber) {
        List<SalesOrder> salesOrders = salesOrderService.getOrderByOrderGroupId(orderNumber).stream()
                .sorted(Comparator.comparing(SalesOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        if (salesOrders.isEmpty()) {
            throw buildNotFoundException(event);
        }
        return salesOrders;
    }

    private boolean isAllIncludedInSkuList(ParcelShipped event, List<String> skuListFromOrderJson) {
        return event.getArticleItemsDtos().stream().allMatch(item -> skuListFromOrderJson.contains(item.getNumber()));
    }

    private boolean hasAnyCombinedItem(ParcelShipped event) {
        return event.getArticleItemsDtos().stream().anyMatch(item
                -> item.getNumber() != null && item.getNumber().contains(COMBINED_ITEM_SEPARATOR));
    }
}
