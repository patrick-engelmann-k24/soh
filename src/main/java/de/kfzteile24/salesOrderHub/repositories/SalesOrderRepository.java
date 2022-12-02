package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

    @Query("SELECT a FROM SalesOrder a WHERE a.orderNumber = :orderNumber")
    Optional<SalesOrder> getOrderByOrderNumber(@Param("orderNumber") String orderNumber);

    List<SalesOrder> findAllByOrderGroupIdOrderByUpdatedAtDesc(String orderGroupId);

    long countByCustomerEmail(String customerEmail);

    @Query("SELECT a.orderNumber FROM SalesOrder a WHERE a.orderGroupId = :orderGroupId")
    List<String> findOrderNumberByOrderGroupId(@Param("orderGroupId") String orderGroupId);
}
