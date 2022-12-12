package de.kfzteile24.salesOrderHub.services.returnorder;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;

import java.util.List;

public interface ReturnOrderCreator {
    List<SalesOrder> getSalesOrderList(String orderNumber);
}
