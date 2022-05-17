package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.soh.order.dto.Order;

import java.util.ArrayList;

abstract public class AbstractSplitDecorator {
    abstract public void processOrderList(ArrayList<Order> orderList);
}
