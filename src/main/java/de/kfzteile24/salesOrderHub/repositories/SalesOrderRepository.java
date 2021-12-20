package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

@Transactional
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

    @Query("SELECT a FROM SalesOrder a WHERE a.orderNumber = :orderNumber")
    Optional<SalesOrder> getOrderByOrderNumber(@Param("orderNumber") String orderNumber);

    @Query("SELECT a FROM SalesOrder a WHERE a.processId = :processId")
    Optional<SalesOrder> getOrderByProcessId(@Param("processId") String processId);

    long countByCustomerEmail(String customerEmail);
}
