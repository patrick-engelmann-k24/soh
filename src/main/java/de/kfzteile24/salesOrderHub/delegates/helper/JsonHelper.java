package de.kfzteile24.salesOrderHub.delegates.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrderItem;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.Rows;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JsonHelper {

    public List<String> getOrderItemsAsStringList(OrderJSON orderJSON) {
        final List<Rows> rows = getOrderJSONItems(orderJSON);
        List<String> orderItemList = new ArrayList<>();
        for (Rows item: rows) {
            orderItemList.add(item.getSku());
        }

        return orderItemList;
    }

    public List<Rows> getOrderJSONItems(OrderJSON orderJSON) {
        return orderJSON.getOrderRows();
    }
}
