package de.kfzteile24.salesOrderHub.services;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ROW_CANCELLED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class SalesOrderServiceIntegrationTest {
    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AuditLogUtil auditLogUtil;

    @Test
    void optimisticLockingPreventsChangesFromBeingOverridden() {
        final var salesOrder = salesOrderService.save(
                SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW), ORDER_CREATED);

        transactionTemplate.execute(status -> {
            final var modifiedSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber())
                    .orElseThrow();

            modifiedSalesOrder.getLatestJson().getOrderRows().get(0).setIsCancelled(true);
            salesOrderService.save(modifiedSalesOrder, ORDER_ROW_CANCELLED);
            return null;
        });

        salesOrder.getLatestJson().getOrderRows().get(1).setIsCancelled(true);
        assertThatThrownBy(() -> salesOrderService.save(salesOrder, ORDER_ROW_CANCELLED))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void savingASalesOrderAddsACorrespondingEntryInTheAudiLogTable() {
        final var salesOrder = salesOrderService.save(
                SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW), ORDER_CREATED);

        auditLogUtil.assertAuditLogExists(salesOrder.getId(), ORDER_CREATED);
    }
}
