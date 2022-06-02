package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.CREDIT_NOTE_NUMBER_SEPARATOR;

@Service
@Slf4j
public class SalesOrderReturnService {

    @Autowired
    private SalesOrderReturnRepository salesOrderReturnRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public SalesOrderReturn getByOrderNumber(String orderNumber) {
        return salesOrderReturnRepository.findByOrderNumber(orderNumber);
    }

    public SalesOrderReturn save(SalesOrderReturn salesOrderReturn) {
        return salesOrderReturnRepository.save(salesOrderReturn);
    }

    public void updateUrl(String orderNumber, String url) {
        salesOrderReturnRepository.updateUrl(orderNumber, url);
    }

    @Transactional
    public SalesOrderReturn save(SalesOrderReturn order, Action action) {
        final var storedOrder = salesOrderReturnRepository.save(order);

        final var auditLog = AuditLog.builder()
                .salesOrderId(storedOrder.getId())
                .action(action)
                .data(order.getReturnOrderJson())
                .build();

        auditLogRepository.save(auditLog);
        return storedOrder;
    }

    public String createCreditNoteNumber() {
        var currentYear = LocalDateTime.now().getYear();
        return currentYear + CREDIT_NOTE_NUMBER_SEPARATOR + getNextCreditNoteCount();
    }

    Optional<String> findLatestCreditNoteNumber() {
        return salesOrderReturnRepository.findLatesCreditNoteNumber();
    }

    String getNextCreditNoteCount() {
        return findLatestCreditNoteNumber().map(this::createNextCreditNoteNumberCounter).orElse("00001");
    }

    private String createNextCreditNoteNumberCounter(String creditNoteNumber) {
        var counter = Long.valueOf(extractCreditNoteCounterNumber(creditNoteNumber));
        counter++;
        return String.format("%05d", counter);
    }

    private String extractCreditNoteCounterNumber(String creditNoteNumber) {
        if (creditNoteNumber.length() != 10)
            throw new IllegalArgumentException("Last return order credit note number in DB does not contain 10 digits");
        return creditNoteNumber.substring(5);
    }
}
