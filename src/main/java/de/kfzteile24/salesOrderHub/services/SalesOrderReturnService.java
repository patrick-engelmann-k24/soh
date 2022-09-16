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

    @Autowired
    private CreditNoteNumberCounterService creditNoteNumberCounterService;

    public SalesOrderReturn getByOrderNumber(String orderNumber) {
        return salesOrderReturnRepository.findByOrderNumber(orderNumber);
    }

    public SalesOrderReturn save(SalesOrderReturn salesOrderReturn) {
        return salesOrderReturnRepository.save(salesOrderReturn);
    }

    @Transactional(readOnly = true)
    public boolean checkOrderNotExists(final String orderNumber) {
        if (getByOrderNumber(orderNumber) != null) {
            log.warn("The following order return won't be processed because it exists in SOH system already from another source. " +
                    "Order Number: {}", orderNumber);
            return false;
        }
        return true;
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
        return createCreditNoteNumber(currentYear);
    }

    String createCreditNoteNumber(int currentYear) {
        return currentYear + CREDIT_NOTE_NUMBER_SEPARATOR + getNextCreditNoteCount(currentYear);
    }

    private String getNextCreditNoteCount(int currentYear) {
        Long creditNoteNumber = creditNoteNumberCounterService.getNextCounter(currentYear);
        return String.format("%05d", creditNoteNumber);
    }
}
