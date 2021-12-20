package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.Set;
import java.util.UUID;

@Transactional
public interface SalesOrderInvoiceRepository extends JpaRepository<SalesOrderInvoice, UUID> {

    Set<SalesOrderInvoice> getInvoicesByOrderNumber(@Param("orderNumber") String orderNumber);

    boolean existsByOrderNumber(String orderNumber);
}
