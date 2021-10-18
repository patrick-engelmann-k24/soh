package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;

import static java.text.MessageFormat.format;
import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRowCancelledDelegate implements JavaDelegate {

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var skuToCancel = (String) delegateExecution.getVariable(RowVariables.ORDER_ROW_ID.getName());

        handleOrderRowCancellation(orderNumber, skuToCancel);

        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        final var latestJson = salesOrder.getLatestJson();
        final boolean isFullCancellation = latestJson.getOrderRows().stream().allMatch(OrderRows::getIsCancelled);
        snsPublishService.publishOrderRowsCancelled(latestJson,
                List.of(findOrderRowToCancel(latestJson, skuToCancel)), isFullCancellation);
    }

    @Transactional(REQUIRES_NEW)
    protected void handleOrderRowCancellation(String orderNumber, String skuToCancel) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));

        findOrderRowToCancel(salesOrder.getLatestJson(), skuToCancel).setIsCancelled(true);
        salesOrderService.save(salesOrder, Action.ORDER_ROW_CANCELLED);
    }

    private OrderRows findOrderRowToCancel(Order latestJson, String skuToCancel) {
        return latestJson.getOrderRows().stream()
                .filter(row -> skuToCancel.equals(row.getSku()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        format("Could not find order row with SKU {0} for order {1}",
                                skuToCancel, latestJson.getOrderHeader().getOrderNumber())));
    }

}
