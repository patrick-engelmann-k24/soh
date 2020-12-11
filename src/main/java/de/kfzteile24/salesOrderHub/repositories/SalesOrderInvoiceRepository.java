package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SalesOrderInvoiceRepository extends JpaRepository<SalesOrderInvoice, UUID> {

    @Query("SELECT inv FROM SalesOrderInvoice inv WHERE inv.salesOrder.orderNumber = :orderNumber")
    List<SalesOrderInvoice> getInvoicesByOrderNumber(@Param("orderNumber") String orderNumber);
}
