package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderReturnRepository extends JpaRepository<SalesOrderReturn, UUID> {

    SalesOrderReturn findByOrderNumber(String orderNumber);
    Optional<SalesOrderReturn> findFirstByOrderGroupIdOrderByCreatedAtDesc(String orderGroupId);
}
