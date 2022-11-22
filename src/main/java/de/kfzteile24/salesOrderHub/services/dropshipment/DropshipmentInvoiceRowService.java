package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DropshipmentInvoiceRowService {

    @NonNull
    private final DropshipmentInvoiceRowRepository dropshipmentInvoiceRowRepository;

    private final DropshipmentHelper dropshipmentHelper;

    public DropshipmentInvoiceRow create(String sku, String orderNumber) {
        final var dropshipmentInvoiceRow = dropshipmentHelper.createDropshipmentInvoiceRow(sku, orderNumber);
        return save(dropshipmentInvoiceRow);
    }

    @Transactional(readOnly = true)
    public Optional<DropshipmentInvoiceRow> getBySkuAndOrderNumber(String sku, String orderNumber) {
        return dropshipmentInvoiceRowRepository.findBySkuAndOrderNumber(sku, orderNumber);
    }

    @Transactional(readOnly = true)
    public List<DropshipmentInvoiceRow> getByOrderNumber(String orderNumber) {
        return dropshipmentInvoiceRowRepository.findByOrderNumber(orderNumber);
    }

    @Transactional
    public DropshipmentInvoiceRow save(DropshipmentInvoiceRow dropshipmentInvoiceRow) {
        return dropshipmentInvoiceRowRepository.save(dropshipmentInvoiceRow);
    }

    @Transactional
    public void deleteAll() {
        dropshipmentInvoiceRowRepository.deleteAll();
    }

    public List<DropshipmentInvoiceRow> findAllOrderByOrderNumberAsc() {
        var list = dropshipmentInvoiceRowRepository.findAllByOrderByOrderNumberAsc();
        log.info("All aggregated invoice data are retrieved from table. Count of entries: {}", list.size());
        return list;
    }

    @Transactional
    public void saveInvoiceNumber(String orderNumber, String invoiceNumber) {
        for (var dropshipmentInvoiceRow : dropshipmentInvoiceRowRepository.findByOrderNumber(orderNumber)) {
            dropshipmentInvoiceRow.setInvoiceNumber(invoiceNumber);
            save(dropshipmentInvoiceRow);
        }
    }
}
