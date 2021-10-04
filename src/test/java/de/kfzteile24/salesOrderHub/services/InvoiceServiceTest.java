package de.kfzteile24.salesOrderHub.services;


import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderInvoice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private SalesOrderInvoiceRepository invoiceRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    public void addingASalesOrderToAnInvoiceUpdatesTheInvoiceAndCreatesAnAuditLogEntry() {
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var invoice = createSalesOrderInvoice(salesOrder.getOrderNumber(), false);
        invoice.setCreatedAt(LocalDateTime.MIN);

        invoiceService.addSalesOrderToInvoice(salesOrder, invoice);

        final var expectedAuditLog = AuditLog.builder()
                .salesOrderId(invoice.getId())
                .action(INVOICE_RECEIVED)
                .data(salesOrder.getLatestJson())
                .createdAt(invoice.getCreatedAt())
                .build();

        verify(auditLogRepository).save(eq(expectedAuditLog));
        verify(invoiceRepository).save(argThat(inv -> {
            assertThat(inv.getSalesOrder()).isEqualTo(salesOrder);
            return true;
        }));
    }

}