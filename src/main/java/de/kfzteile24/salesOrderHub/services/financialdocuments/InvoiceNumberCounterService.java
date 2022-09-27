package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.domain.InvoiceNumberCounter;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
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
public class InvoiceNumberCounterService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private InvoiceNumberCounterRepository invoiceNumberCounterRepository;

    @PostConstruct
    public void init() {
        var currentYear = LocalDateTime.now().getYear();
        for (int year = currentYear; year <= currentYear + 2; year++) {
            var optional = invoiceNumberCounterRepository.findById(year);
            if (optional.isEmpty()) {
                var invoiceNumberCounter = InvoiceNumberCounter.builder()
                        .counter(Long.valueOf(1))
                        .year(year)
                        .build();
                invoiceNumberCounterRepository.save(invoiceNumberCounter);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long getNextCounter(Integer currentYear) {
        InvoiceNumberCounter invoiceNumberCounter = entityManager.find(InvoiceNumberCounter.class, currentYear, LockModeType.PESSIMISTIC_WRITE,
                Collections.singletonMap("javax.persistence.lock.timeout",
                        TimeUnit.SECONDS.toMillis(5)));
        Long result = invoiceNumberCounter.getCounter();
        invoiceNumberCounter.setCounter(result + 1);
        invoiceNumberCounterRepository.save(invoiceNumberCounter);
        return result;
    }

}
