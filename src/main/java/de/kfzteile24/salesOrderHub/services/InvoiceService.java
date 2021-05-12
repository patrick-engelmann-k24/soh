package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    @NonNull
    private final SalesOrderInvoiceRepository invoiceRepository;

    /**
     * If we find an invoice, there are already invoice(s) created
     *
     * @param orderNumber
     * @return
     */
    public Boolean checkInvoiceExistentForOrder(final String orderNumber) {
        final List<SalesOrderInvoice> orderInvoiceList = invoiceRepository.getInvoicesByOrderNumber(orderNumber);
        return orderInvoiceList.size() > 0;
    }
}
