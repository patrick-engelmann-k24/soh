package de.kfzteile24.salesOrderHub.helper;

import com.newrelic.api.agent.Insights;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MetricsHelper {

    private final Insights insights;

    public void salesOrderConsumed(SalesOrder salesOrder) {
        var order = salesOrder.getLatestJson();
        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("Platform", order.getOrderHeader().getPlatform().name());
        eventAttributes.put("SalesChannel", order.getOrderHeader().getSalesChannel());
        eventAttributes.put("SalesOrderNumber", salesOrder.getOrderNumber());
        insights.recordCustomEvent("SohSalesOrderConsumed", eventAttributes);
    }

    public void splittedOrderGenerated(SalesOrder salesOrder) {
        var order = salesOrder.getLatestJson();
        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("Platform", order.getOrderHeader().getPlatform().name());
        eventAttributes.put("SalesChannel", order.getOrderHeader().getSalesChannel());
        eventAttributes.put("SalesOrderNumber", salesOrder.getOrderNumber());
        insights.recordCustomEvent("SohSplitOrderGenerated", eventAttributes);
    }

}
