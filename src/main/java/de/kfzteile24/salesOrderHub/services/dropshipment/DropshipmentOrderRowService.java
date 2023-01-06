package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.InvoiceData;
import de.kfzteile24.salesOrderHub.exception.DropshipmentOrderRowNotFoundException;
import de.kfzteile24.salesOrderHub.exception.InvoiceNotFoundException;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentOrderRowRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
public class DropshipmentOrderRowService {

    @NonNull
    private final DropshipmentOrderRowRepository dropshipmentOrderRowRepository;

    private final DropshipmentHelper dropshipmentHelper;

    public DropshipmentOrderRow create(String sku, String orderNumber) {
        final var dropshipmentOrderRow = dropshipmentHelper.createDropshipmentOrderRow(sku, orderNumber);
        return save(dropshipmentOrderRow);
    }

    @Transactional(readOnly = true)
    public Optional<DropshipmentOrderRow> getBySkuAndOrderNumber(String sku, String orderNumber) {
        return dropshipmentOrderRowRepository.findBySkuAndOrderNumber(sku, orderNumber);
    }

    @Transactional(readOnly = true)
    public Integer getQuantityBySkuAndOrderNumber(String sku, String orderNumber) {
        return (dropshipmentOrderRowRepository.findBySkuAndOrderNumber(sku, orderNumber)).get().getQuantity();
    }

    @Transactional(readOnly = true)
    public Integer getQuantityShippedBySkuAndOrderNumber(String sku, String orderNumber) {
        return (dropshipmentOrderRowRepository.findBySkuAndOrderNumber(sku, orderNumber)).get().getQuantityShipped();
    }

    @Transactional(readOnly = true)
    public List<DropshipmentOrderRow> getByOrderNumber(String orderNumber) {
        return dropshipmentOrderRowRepository.findByOrderNumber(orderNumber);
    }

    @Transactional
    public DropshipmentOrderRow save(DropshipmentOrderRow dropshipmentOrderRow) {
        return dropshipmentOrderRowRepository.save(dropshipmentOrderRow);
    }

    @Transactional
    public void deleteAll() {
        dropshipmentOrderRowRepository.deleteAll();
    }

    public List<DropshipmentOrderRow> findAllOrderByOrderNumberAsc() {
        var list = dropshipmentOrderRowRepository.findAllByOrderByOrderNumberAsc();
        log.info("All aggregated dropshipment order row data are retrieved from table. Count of entries: {}", list.size());
        return list;
    }

    @Transactional
    public void saveQuantityShipped(String sku, String orderNumber, Integer quantityShipped) {
        if (quantityShipped == null || quantityShipped.equals(0)) {
            throw new IllegalArgumentException("Shipped Quantity must not be null or zero, when updating Dropshipment Order Row with saveQuantityShipped method");
        }
        var dropshipmentOrderRow = dropshipmentOrderRowRepository.findBySkuAndOrderNumber(sku, orderNumber)
                .orElseThrow(() -> new DropshipmentOrderRowNotFoundException(sku, orderNumber));
        log.info("Dropshipment Order Row with sku: {} and order number: {} is updated with shipped quantity: {}", sku, orderNumber, quantityShipped);
        dropshipmentOrderRow.setQuantityShipped(quantityShipped);
        save(dropshipmentOrderRow);
    }

}
