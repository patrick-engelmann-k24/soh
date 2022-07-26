package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.InvoiceNumberCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceNumberCounterRepository extends JpaRepository<InvoiceNumberCounter, Integer> {

}
