package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.InvoiceData;
import de.kfzteile24.salesOrderHub.exception.InvoiceNotFoundException;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DropshipmentInvoiceRowService {

    @NonNull
    private final DropshipmentInvoiceRowRepository dropshipmentInvoiceRowRepository;

    private final DropshipmentHelper dropshipmentHelper;

    @Transactional
    public DropshipmentInvoiceRow create(String sku, String orderNumber, int quantity) {
        final var dropshipmentInvoiceRow = dropshipmentHelper.createDropshipmentInvoiceRow(sku, orderNumber, quantity);
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

    @Transactional
    public void delete(DropshipmentInvoiceRow dropshipmentInvoiceRow) {
        dropshipmentInvoiceRowRepository.delete(dropshipmentInvoiceRow);
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

    @Transactional(readOnly = true)
    public InvoiceData getInvoiceData(String invoiceNumber) {

        List<DropshipmentInvoiceRow> invoiceData = getByInvoiceNumber(invoiceNumber);

        return buildInvoiceDataForIndividualOrderNumber(invoiceData, invoiceNumber);
    }

    /**
     * returns invoice data object for ONLY ONE invoice number results from dropshipment invoice row entity list
     */
    public InvoiceData buildInvoiceDataForIndividualOrderNumber(List<DropshipmentInvoiceRow> dropshipmentInvoiceRows, String invoiceNumber) {

        if (dropshipmentInvoiceRows == null || dropshipmentInvoiceRows.isEmpty()) {
            throw new NotFoundException("Matching invoice row not found for the given invoice number " + invoiceNumber);
        }

        val orderNumber = dropshipmentInvoiceRows.get(0).getOrderNumber();
        val orderRows = dropshipmentInvoiceRows.stream().map(DropshipmentInvoiceRow::getSku).collect(Collectors.toList());
        val quantities = dropshipmentInvoiceRows.stream().map(DropshipmentInvoiceRow::getQuantity).collect(Collectors.toList());

        return InvoiceData.builder()
                .orderNumber(orderNumber)
                .invoiceNumber(invoiceNumber)
                .orderRows(orderRows)
                .quantities(quantities)
                .build();
    }

    public Set<String> buildOrderNumberSet(Collection<DropshipmentInvoiceRow> dropshipmentInvoiceRows) {
        Set<String> result = new TreeSet<>();
        dropshipmentInvoiceRows.forEach(item -> {
            result.add(item.getOrderNumber());
        });
        return result;
    }

    @Transactional
    public List<DropshipmentInvoiceRow> mergeRowsByOrderNumberAndSku(List<DropshipmentInvoiceRow> dropshipmentInvoiceRows) {
        Map<String, Map<String, List<DropshipmentInvoiceRow>>> rowsMap = new TreeMap<>();
        dropshipmentInvoiceRows.forEach(item -> {
            val orderNumberKey = item.getOrderNumber();
            val skuKey = item.getSku();
            rowsMap.computeIfAbsent(orderNumberKey, k -> new TreeMap<>())
                    .computeIfAbsent(skuKey, k -> new ArrayList<>()).add(item);
        });
        val result = new ArrayList<DropshipmentInvoiceRow>();
        for (val maps : rowsMap.values()) {
            for (val rows: maps.values()) {
                val mainRow = rows.get(0);
                if (rows.size() > 1) {
                    for (int i = 1; i < rows.size(); i++) {
                        val row = rows.get(i);
                        mainRow.addQuantity(row.getQuantity());
                        delete(row);
                    }
                    save(mainRow);
                }
                result.add(mainRow);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public String getOrderNumberByInvoiceNumber(String invoiceNumber) {
        List<DropshipmentInvoiceRow> invoiceDataList = getByInvoiceNumber(invoiceNumber);
        if (CollectionUtils.isEmpty(invoiceDataList)) {
            throw new InvoiceNotFoundException(invoiceNumber);
        }
        return invoiceDataList.get(0).getOrderNumber();
    }
}
