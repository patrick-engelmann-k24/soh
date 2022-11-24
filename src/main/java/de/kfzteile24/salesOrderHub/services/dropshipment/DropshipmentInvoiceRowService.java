package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.InvoiceData;
import de.kfzteile24.salesOrderHub.exception.InvoiceNotFoundException;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<DropshipmentInvoiceRow> getByInvoiceNumber(String invoiceNumber) {
        return dropshipmentInvoiceRowRepository.findByInvoiceNumber(invoiceNumber);
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

    public InvoiceData getInvoiceData(String invoiceNumber) {

        List<DropshipmentInvoiceRow> invoiceData =
                getByInvoiceNumber(invoiceNumber);

        if (invoiceData == null || invoiceData.size() == 0) {
            throw new InvoiceNotFoundException(invoiceNumber);
        }

        return generateInvoiceData(invoiceData);
    }

    public InvoiceData generateInvoiceData(List<DropshipmentInvoiceRow> dropshipmentInvoiceRows) {
        var orderNumber = dropshipmentInvoiceRows.get(0).getOrderNumber();
        var invoiceNumber = dropshipmentInvoiceRows.get(0).getInvoiceNumber();
        var orderRows = dropshipmentInvoiceRows.stream().map(DropshipmentInvoiceRow::getSku).collect(Collectors.toList());

        return InvoiceData.builder()
                .orderNumber(orderNumber)
                .invoiceNumber(invoiceNumber)
                .orderRows(orderRows)
                .build();
    }

    public Map<String, List<String>> generateInvoiceDataMap(Collection<DropshipmentInvoiceRow> dropshipmentInvoiceRows) {
        Map<String, List<String>> dropshipmentInvoiceRowMap = new TreeMap<>();
        dropshipmentInvoiceRows.forEach(item -> {
            var key = item.getOrderNumber();
            var value = item.getSku();
            var valueList = dropshipmentInvoiceRowMap.get(key);
            if (valueList == null) {
                valueList = new ArrayList<>(List.of(value));
            } else {
                valueList.add(value);
            }
            dropshipmentInvoiceRowMap.put(key, valueList);
        });
        return dropshipmentInvoiceRowMap;
    }
}
