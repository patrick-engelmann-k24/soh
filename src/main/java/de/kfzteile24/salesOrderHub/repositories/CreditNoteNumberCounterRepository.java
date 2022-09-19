package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.CreditNoteNumberCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteNumberCounterRepository extends JpaRepository<CreditNoteNumberCounter, Integer> {

}
