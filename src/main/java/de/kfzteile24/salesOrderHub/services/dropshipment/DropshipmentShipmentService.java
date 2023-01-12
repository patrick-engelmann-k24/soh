package de.kfzteile24.salesOrderHub.services.dropshipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_SHIPMENT_CONFIRMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_FULLY_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.QUANTITY_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ITEM_SHIPPED;
import static java.text.MessageFormat.format;

@Service
@Slf4j
@RequiredArgsConstructor
public class DropshipmentShipmentService {

    private final SalesOrderService salesOrderService;
    private final ObjectMapper objectMapper;
    private final CamundaHelper camundaHelper;

    @EnrichMessageForDlq
    public void handleDropshipmentShipmentConfirmed(
            DropshipmentShipmentConfirmedMessage message, MessageWrapper messageWrapper) {

        val orderNumber = message.getSalesOrderNumber();
        SalesOrder salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        val shippedItems = message.getItems();
        validateShippedItems(shippedItems);
        val orderRows = salesOrder.getLatestJson().getOrderRows();

        val savedSalesOrder = updateSalesOrderWithTrackingInformation(
                salesOrder, shippedItems, orderRows);

        startDropshipmentShipmentProcess(savedSalesOrder, shippedItems, orderRows);
    }

    private void validateShippedItems(Collection<ShipmentItem> shippedItems) {
        for (ShipmentItem item: shippedItems) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Shipped Quantity must not be null or zero, when handling " +
                        "DropshipmentShipmentConfirmed event. ");
            }
        }
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

    @SneakyThrows
    private String getTrackingLink(ShipmentItem shipmentItem, Map<String, List<String>> skuMap) {
        return objectMapper.writeValueAsString(TrackingLink.builder()
                .url(shipmentItem.getTrackingLink())
                .orderItems(skuMap.get(shipmentItem.getTrackingLink()))
                .build());
    }

    private void startDropshipmentShipmentProcess(SalesOrder savedSalesOrder,
                                                  Collection<ShipmentItem> shippedItems,
                                                  List<OrderRows> orderRows) {
        val skuMap = getSkuMap(shippedItems);
        shippedItems.forEach(item ->
                orderRows.stream()
                        .filter(row -> StringUtils.pathEquals(row.getSku(), item.getProductNumber()))
                        .forEach(row -> {
                            val businessKey = format("{0}#{1}#{2}", savedSalesOrder.getOrderNumber(),
                                    row.getSku(), item.getTrackingLink());
                            val variablesMap = Variables
                                    .putValue(ORDER_NUMBER.getName(), savedSalesOrder.getOrderNumber())
                                    .putValue(ORDER_ROW.getName(), row.getSku())
                                    .putValue(QUANTITY_SHIPPED.getName(), item.getQuantity())
                                    .putValue(TRACKING_LINKS.getName(), List.of(getTrackingLink(item, skuMap)))
                                    .putValue(ORDER_FULLY_SHIPPED.getName(), savedSalesOrder.isShipped());
                            camundaHelper.startProcessByMessage(DROPSHIPMENT_SHIPMENT_CONFIRMATION_RECEIVED,
                                    businessKey, variablesMap);
                            log.info("Dropshipment shipment process started. Variables: {}", variablesMap);
                        })
        );
    }

    /*
        This method groups the sku names according to tracking link information if the tracking link is the same for multiple sku
     */
    private static Map<String, List<String>> getSkuMap(Collection<ShipmentItem> shippedItems) {
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

    private static void addParcelNumber(ShipmentItem item, OrderRows row) {
        var parcelNumber = item.getParcelNumber();
        Optional.ofNullable(row.getTrackingNumbers())
                .ifPresentOrElse(trackingNumbers -> trackingNumbers.add(parcelNumber),
                        () -> {
                            if (Objects.isNull(row.getTrackingNumbers())) {
                                row.setTrackingNumbers(new ArrayList<>());
                            }
                            row.getTrackingNumbers().add(parcelNumber);
                        });
    }

    private static void addServiceProviderName(ShipmentItem item, OrderRows row) {
        row.setShippingProvider(item.getServiceProviderName());
    }
}
