package de.kfzteile24.salesOrderHub.services.returnorder;

import de.kfzteile24.salesOrderHub.constants.SalesOrderType;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReturnOrderServiceAdaptor {
    private final RegularReturnOrderHandler regularReturnOrderHandler;
    private final DropshipmentReturnOrderHandler dropshipmentReturnOrderHandler;

    public List<SalesOrder> getSalesOrderList(String orderGroupId, SalesOrderType salesOrderType) {
        if (salesOrderType.equals(SalesOrderType.DROPSHIPMENT)) {
            return dropshipmentReturnOrderHandler.getSalesOrderList(orderGroupId);
        } else {
            return regularReturnOrderHandler.getSalesOrderList(orderGroupId);
        }
    }
}
