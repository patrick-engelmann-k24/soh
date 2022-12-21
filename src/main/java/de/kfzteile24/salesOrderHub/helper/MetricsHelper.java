package de.kfzteile24.salesOrderHub.helper;

import com.newrelic.api.agent.Insights;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.constants.CustomEventName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MetricsHelper {

    private final Insights insights;

    public void sendCustomEvent(SalesOrder salesOrder, CustomEventName customEventName) {
        var order = salesOrder.getLatestJson();
        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("Platform", order.getOrderHeader().getPlatform().name());
        eventAttributes.put("SalesChannel", order.getOrderHeader().getSalesChannel());
        eventAttributes.put("SalesOrderNumber", order.getOrderHeader().getOrderNumber());
        sendCustomEvent(customEventName, eventAttributes);
    }

    public void sendCustomEventForInvoices(SalesOrder salesOrder, CustomEventName customEventName) {
        var invoiceEvent = salesOrder.getInvoiceEvent();
        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("OrderNumber", salesOrder.getOrderNumber());
        eventAttributes.put("OrderGroupId", salesOrder.getOrderGroupId());
        eventAttributes.put("InvoiceNumber", invoiceEvent.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
        sendCustomEvent(customEventName, eventAttributes);
    }

    public void sendCustomEventForDropshipmentOrder(SalesOrder salesOrder, CustomEventName customEventName) {
        var order = salesOrder.getLatestJson();
        var invoiceEvent = salesOrder.getInvoiceEvent();
        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("OrderNumber", salesOrder.getOrderNumber());
        eventAttributes.put("OrderGroupId", salesOrder.getOrderGroupId());
        eventAttributes.put("Platform", order.getOrderHeader().getPlatform().name());
        eventAttributes.put("SalesChannel", order.getOrderHeader().getSalesChannel());
        eventAttributes.put("TotalGrossAmount", order.getOrderHeader().getTotals().getGrandTotalGross());
        eventAttributes.put("TotalNetAmount", order.getOrderHeader().getTotals().getGrandTotalNet());
        sendCustomEvent(customEventName, eventAttributes);
    }

    public void sendCustomEvent(CustomEventName customEventName, Map<String, Object> eventAttributes) {
        insights.recordCustomEvent(customEventName.getName(), eventAttributes);
    }
}
