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

    SalesOrderReturn findByOrderNumber(String orderNumber);
    Optional<SalesOrderReturn> findFirstByOrderGroupIdOrderByCreatedAtDesc(String orderGroupId);
    @Query(value = "SELECT json_extract_path_text(ro.credit_note_event , 'SalesCreditNote', 'SalesCreditNoteHeader', 'CreditNoteNumber') "
            + "FROM return_order ro "
            + "WHERE json_extract_path_text(ro.credit_note_event , 'SalesCreditNote') IS NOT NULL "
            + "ORDER BY json_extract_path_text(ro.credit_note_event , 'SalesCreditNote', 'SalesCreditNoteHeader', 'CreditNoteNumber') DESC "
            + "LIMIT 1", nativeQuery = true)
    Optional<String> findLatesCreditNoteNumber();

    @Modifying
    @Query("update SalesOrderReturn return set return.url = :url where return.orderNumber = :orderNumber")
    void updateUrl(@Param("orderNumber") String orderNumber, @Param("url") String url);
}
