package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DropshipmentInvoiceRowRepository extends JpaRepository<DropshipmentInvoiceRow, UUID> {

    Optional<DropshipmentInvoiceRow> findBySkuAndOrderNumber(String sku, String orderNumber);

    List<DropshipmentInvoiceRow> findByOrderNumber(String orderNumber);

}
