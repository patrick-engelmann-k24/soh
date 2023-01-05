package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DropshipmentOrderRowRepository extends JpaRepository<DropshipmentOrderRow, UUID> {

    Optional<DropshipmentOrderRow> findBySkuAndOrderNumber(String sku, String orderNumber);

    List<DropshipmentOrderRow> findByOrderNumber(String orderNumber);

    List<DropshipmentOrderRow> findAllByOrderByOrderNumberAsc();
}
