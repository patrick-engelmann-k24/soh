package de.kfzteile24.salesOrderHub.services.general;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShipped;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.COMBINED_ITEM_SEPARATOR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ITEM_SHIPPED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelShippedService {

    private final SnsPublishService snsPublishService;
    private final SalesOrderService salesOrderService;

    @EnrichMessageForDlq
    public void handleParcelShipped(ParcelShippedMessage message, MessageWrapper messageWrapper) {
        var event = message.getMessage();
        var orderNumber = event.getOrderNumber();
        log.info("Parcel shipped received with order number {}, delivery note number {}, " +
                        "tracking link: {} and order items: {}",
                orderNumber,
                event.getDeliveryNoteNumber(),
                event.getTrackingLink(),
                event.getArticleItemsDtos().stream()
                        .map(ArticleItemsDto::getNumber)
                        .collect(Collectors.toList()));

        List<SalesOrder> salesOrders = getSalesOrdersByGroupId(event);
        updateShippingProvider(event, salesOrders);
        handleParcelShippedEvent(event, salesOrders);
    }

    private List<SalesOrder> getSalesOrdersByGroupId(ParcelShipped event) {
        List<SalesOrder> salesOrders = salesOrderService.getOrderByOrderGroupId(event.getOrderNumber()).stream()
                .sorted(Comparator.comparing(SalesOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        if (salesOrders.isEmpty()) {
            throw new NotFoundException(
                    MessageFormat.format("There is no sales order for order group id {0}", event.getOrderNumber()));
        }
        return salesOrders;
    }

    private void updateShippingProvider(ParcelShipped event, List<SalesOrder> salesOrders) {
        if (event.getLogisticsPartnerName() != null) {
            for (SalesOrder salesOrder : salesOrders) {
                for (OrderRows orderRow : salesOrder.getLatestJson().getOrderRows()) {
                    for (ArticleItemsDto item : event.getArticleItemsDtos()) {
                        if (item.getNumber().equals(orderRow.getSku())) {
                            orderRow.setShippingProvider(event.getLogisticsPartnerName());
                        }
                    }
                }
                salesOrderService.save(salesOrder, ORDER_ITEM_SHIPPED);
            }
        }
    }


    private boolean isCorePlatformOrder(List<SalesOrder> salesOrders) {
        return Platform.CORE == ((Order)salesOrders.get(0).getOriginalOrder()).getOrderHeader().getPlatform();
    }

    protected void handleParcelShippedEvent(ParcelShipped event, List<SalesOrder> salesOrders) {
        var orderNumber = event.getOrderNumber();
        if (hasAnyCombinedItem(event)) {
            log.info("Order: {} has combined items, so it would be ignored for ParcelShippedEvent Handling", orderNumber);
        } else {
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

    private boolean isAllIncludedInSkuList(ParcelShipped event, List<String> skuListFromOrderJson) {
        return event.getArticleItemsDtos().stream().allMatch(item -> skuListFromOrderJson.contains(item.getNumber()));
    }

    private boolean hasAnyCombinedItem(ParcelShipped event) {
        return event.getArticleItemsDtos().stream().anyMatch(item
                -> item.getNumber() != null && item.getNumber().contains(COMBINED_ITEM_SEPARATOR));
    }
}
