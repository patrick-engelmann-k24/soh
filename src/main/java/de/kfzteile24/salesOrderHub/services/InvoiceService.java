package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    @NonNull
    private final SalesOrderInvoiceRepository invoiceRepository;

    @NonNull
    private final AuditLogRepository auditLogRepository;

    /**
     * If we find an invoice, there are already invoice(s) created
     *
     * @param orderNumber
     * @return
     */
    public boolean checkInvoiceExistsForOrder(final String orderNumber) {
        return invoiceRepository.existsByOrderNumber(orderNumber);
    }

    public Set<SalesOrderInvoice> getInvoicesByOrderNumber(String orderNumber) {
        return invoiceRepository.getInvoicesByOrderNumber(orderNumber);
    }

    public SalesOrderInvoice saveInvoice(SalesOrderInvoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public SalesOrderInvoice addSalesOrderToInvoice(SalesOrder salesOrder, SalesOrderInvoice invoice) {
        final var auditLog = AuditLog.builder()
                .salesOrderId(invoice.getId())
                .action(INVOICE_RECEIVED)
                .data(salesOrder.getLatestJson())
                .createdAt(invoice.getCreatedAt())
                .build();

        auditLogRepository.save(auditLog);

        invoice.setSalesOrder(salesOrder);
        return saveInvoice(invoice);
    }
}
