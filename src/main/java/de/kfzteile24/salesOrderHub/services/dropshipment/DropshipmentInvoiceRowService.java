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

    @Transactional(readOnly = true)
    public InvoiceData getInvoiceData(String invoiceNumber) {

        List<DropshipmentInvoiceRow> invoiceData = getByInvoiceNumber(invoiceNumber);

        return buildInvoiceDataForIndividualOrderNumber(invoiceData, invoiceNumber);
    }

    @Transactional(readOnly = true)
    public Integer getOrderRowQuantity(String orderNumber, String orderRow) {
        Integer sumQuantity = 0;
        List<DropshipmentInvoiceRow> allSkuAndOrderNumber
                = dropshipmentInvoiceRowRepository.findAllBySkuAndOrderNumber(orderRow, orderNumber);
        // {"sku1, orderNumber1, invoiceNumber, 1","sku1, orderNumber1, invoiceNumber, 1"}
        for (Object dropshipmentInvoiceRow : allSkuAndOrderNumber){
            sumQuantity += getOrderRowQuantity(orderNumber, orderRow);
        }
        return sumQuantity;
        // sumQuantity for (orderNumber1, sku1) : 2
    }
    /**
     * returns invoice data object for ONLY ONE invoice number results from dropshipment invoice row entity list
     */
    public InvoiceData buildInvoiceDataForIndividualOrderNumber(List<DropshipmentInvoiceRow> dropshipmentInvoiceRows, String invoiceNumber) {

        if (dropshipmentInvoiceRows == null || dropshipmentInvoiceRows.isEmpty()) {
            throw new NotFoundException("Matching invoice row not found for the given invoice number " + invoiceNumber);
        }

        var orderNumber = dropshipmentInvoiceRows.get(0).getOrderNumber();
        var orderRows = dropshipmentInvoiceRows.stream().map(DropshipmentInvoiceRow::getSku).collect(Collectors.toList());
        // orderRows fetched based on the orderNumber, e.g. {"sku1", "sku45"}

        List<Pair<String, Integer>> orderRowAndQuantityList = new ArrayList<>();
        for (String orderRow : orderRows) {
            orderRowAndQuantityList.add(Pair.of(orderRow, getOrderRowQuantity(orderNumber, orderRow)));
        }
        // {("sku1", 2"), ("sku45", 1")} in case of invoice rows ("sku1", .. "1"),("sku1", .. "1"),("sku45", .. "1"),

        return InvoiceData.builder()
                .orderNumber(orderNumber)
                .invoiceNumber(invoiceNumber)
                .orderRows(orderRows)
                .orderRowAndQuantity(orderRowAndQuantityList)
                .build();
    }

    public Map<String, List<String>> buildInvoiceDataMap(Collection<DropshipmentInvoiceRow> dropshipmentInvoiceRows) {
        Map<String, List<String>> dropshipmentInvoiceRowMap = new TreeMap<>();
        dropshipmentInvoiceRows.forEach(item -> {
            var key = item.getOrderNumber();
            var value = item.getSku();
            var valueList = dropshipmentInvoiceRowMap.computeIfAbsent(key, k -> new ArrayList<>());
            valueList.add(value);
            dropshipmentInvoiceRowMap.put(key, valueList);
        });
        return dropshipmentInvoiceRowMap;
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
