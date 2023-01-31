package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderReturnRepository extends JpaRepository<SalesOrderReturn, UUID> {

    Optional<SalesOrderReturn> findByOrderNumber(String orderNumber);

    Optional<SalesOrderReturn> findFirstByOrderNumber(String orderNumber);

    @Modifying
    @Query("update SalesOrderReturn return set return.url = :url where return.orderNumber = :orderNumber")
    void updateUrl(@Param("orderNumber") String orderNumber, @Param("url") String url);
}
