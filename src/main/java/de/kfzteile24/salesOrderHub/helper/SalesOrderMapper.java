package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.soh.order.dto.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SalesOrderMapper {

    @Mapping(expression = "java(Double.valueOf(order.getVersion()).longValue())", target = "version")
    @Mapping(expression = "java((Order) order)", target = "originalOrder")
    @Mapping(source = "order", target = "latestJson")
    @Mapping(source = "order.orderHeader", target = ".")
    @Mapping(source = "order.orderHeader.customer", target = ".")
    SalesOrder map(Order order);
}
