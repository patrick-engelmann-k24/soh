package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SalesOrderInvoiceRepository extends JpaRepository<SalesOrderInvoice, UUID> {

    Set<SalesOrderInvoice> getInvoicesByOrderNumber(@Param("orderNumber") String orderNumber);

    Set<SalesOrderInvoice> getInvoicesByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

    Optional<SalesOrderInvoice> findFirstBySourceAndInvoiceNumberStartsWithOrderByInvoiceNumberDesc(InvoiceSource source, String invoiceNumberStartsWith);

    boolean existsByOrderNumber(String orderNumber);
}
