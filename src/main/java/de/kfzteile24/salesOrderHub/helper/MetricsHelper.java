package de.kfzteile24.salesOrderHub.helper;

import com.newrelic.api.agent.NewRelic;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MetricsHelper {

    public void salesOrderConsumed(Order order) {

        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("Platform", order.getOrderHeader().getPlatform().name());
        eventAttributes.put("SalesChannel", order.getOrderHeader().getSalesChannel());
        eventAttributes.put("SalesOrderNumber", order.getOrderHeader().getOrderNumber());
        NewRelic.getAgent().getInsights().recordCustomEvent("SohSalesOrderConsumed", eventAttributes);
    }

}
