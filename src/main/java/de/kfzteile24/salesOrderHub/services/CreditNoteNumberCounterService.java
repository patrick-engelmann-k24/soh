package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.CreditNoteNumberCounter;
import de.kfzteile24.salesOrderHub.repositories.CreditNoteNumberCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CreditNoteNumberCounterService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CreditNoteNumberCounterRepository creditNoteNumberCounterRepository;

    @PostConstruct
    public void init() {
        var currentYear = LocalDateTime.now().getYear();
        for (int year = currentYear; year <= currentYear + 2; year++) {
            var optional = creditNoteNumberCounterRepository.findById(year);
            if (optional.isEmpty()) {
                var creditNoteNumberCounter = CreditNoteNumberCounter.builder()
                        .counter(Long.valueOf(1))
                        .year(year)
                        .build();
                creditNoteNumberCounterRepository.save(creditNoteNumberCounter);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long getNextCounter(Integer currentYear) {
        CreditNoteNumberCounter creditNoteNumberCounter = entityManager.find(CreditNoteNumberCounter.class, currentYear, LockModeType.PESSIMISTIC_WRITE,
                Collections.singletonMap("javax.persistence.lock.timeout",
                        TimeUnit.SECONDS.toMillis(5)));
        Long result = creditNoteNumberCounter.getCounter();
        creditNoteNumberCounter.setCounter(result + 1);
        creditNoteNumberCounterRepository.save(creditNoteNumberCounter);
        return result;
    }

}
