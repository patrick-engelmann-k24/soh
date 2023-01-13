package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.dto.dropshipment.DropshipmentItemQuantity;
import de.kfzteile24.salesOrderHub.dto.dropshipment.DropshipmentOrderShipped;
import de.kfzteile24.salesOrderHub.exception.DropshipmentOrderRowNotFoundException;
import de.kfzteile24.salesOrderHub.exception.DropshipmentOrderRowOverShippedException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentOrderRowRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.DropshipmentCreateUpdateShipmentDataDelegate.calculateQuantityToBeInvoiced;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_ORDER_SHIPPED;

@Service
@Slf4j
@RequiredArgsConstructor
public class DropshipmentOrderRowService {

    @NonNull
    private final DropshipmentOrderRowRepository dropshipmentOrderRowRepository;

    @NotNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @NonNull
    private final DropshipmentHelper dropshipmentHelper;

    @NonNull
    private final SalesOrderService salesOrderService;

    @NotNull
    private final CamundaHelper camundaHelper;

    @Transactional
    public void shipItems(DropshipmentOrderShipped dropshipmentOrderShipped) {
        var orderNumber = dropshipmentOrderShipped.getOrderNumber();
        for (DropshipmentItemQuantity itemQuantity : dropshipmentOrderShipped.getItems()) {
            var sku = itemQuantity.getSku();
            var quantity = itemQuantity.getQuantity();
            DropshipmentOrderRow dropshipmentOrderRow =
                    dropshipmentOrderRowRepository.findBySkuAndOrderNumber(sku, orderNumber)
                            .orElseThrow(() -> new DropshipmentOrderRowNotFoundException(sku, orderNumber));
            var totalQuantity = quantity + dropshipmentOrderRow.getQuantityShipped();
            if (totalQuantity > dropshipmentOrderRow.getQuantity()) {
                throw new DropshipmentOrderRowOverShippedException(sku, orderNumber, quantity);
            } else {
                dropshipmentOrderRow = addQuantityShipped(sku, orderNumber, quantity);
                var quantityToBeInvoiced = calculateQuantityToBeInvoiced(quantity, dropshipmentOrderRow);
                dropshipmentInvoiceRowService.create(sku, orderNumber, quantityToBeInvoiced);
            }
        }

        var isFullyShipped = isItemsFullyShipped(orderNumber);
        if (isFullyShipped) {
            camundaHelper.correlateMessage(Messages.DROPSHIPMENT_ORDER_FULLY_COMPLETED, orderNumber);
        }
    }

    @Transactional
    public void saveDropshipmentOrderItems(String orderNumber) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find dropshipment order: " + orderNumber));
        if (dropshipmentOrderRowRepository.countByOrderNumber(orderNumber) == 0) {
            for (OrderRows orderRows : salesOrder.getLatestJson().getOrderRows()) {
                create(orderRows.getSku(), orderNumber, orderRows.getQuantity().intValue());
            }
        }
    }

    @Transactional
    public DropshipmentOrderRow create(String sku, String orderNumber, int quantity) {
        final var dropshipmentOrderRow = dropshipmentHelper.createDropshipmentOrderRow(sku, orderNumber, quantity);
        return save(dropshipmentOrderRow);
    }

    @Transactional(readOnly = true)
    public Optional<DropshipmentOrderRow> getBySkuAndOrderNumber(String sku, String orderNumber) {
        return dropshipmentOrderRowRepository.findBySkuAndOrderNumber(sku, orderNumber);
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

    @Transactional
    public DropshipmentOrderRow addQuantityShipped(String sku, String orderNumber, Integer quantityShipped) {
        if (quantityShipped == null || quantityShipped.equals(0)) {
            throw new IllegalArgumentException("Shipped Quantity must not be null or zero, when updating Dropshipment Order Row with addQuantityShipped method");
        }
        var dropshipmentOrderRow = dropshipmentOrderRowRepository.findBySkuAndOrderNumber(sku, orderNumber)
                .orElseThrow(() -> new DropshipmentOrderRowNotFoundException(sku, orderNumber));
        log.info("Dropshipment Order Row with sku: {} and order number: {} is updated with shipped quantity: {}", sku, orderNumber, quantityShipped);
        dropshipmentOrderRow.addQuantityShipped(quantityShipped);
        return save(dropshipmentOrderRow);
    }

    @Transactional
    public boolean isItemsFullyShipped(String orderNumber) {
        var dropshipmentOrderRows = getByOrderNumber(orderNumber);
        if (dropshipmentOrderRows.isEmpty()) {
            return false;
        }
        for (DropshipmentOrderRow dropshipmentOrderRow : dropshipmentOrderRows) {
            if (dropshipmentOrderRow.getQuantityShipped() < dropshipmentOrderRow.getQuantity()) {
                return false;
            }
        }
        updateSalesOrderAsShipped(orderNumber);
        return true;
    }

    private void updateSalesOrderAsShipped(String orderNumber) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find dropshipment order: " + orderNumber));
        salesOrder.setShipped(true);
        salesOrderService.save(salesOrder, DROPSHIPMENT_ORDER_SHIPPED);
    }

    @Transactional(readOnly = true)
    public List<String> getSkuListToBeCancelled(String orderNumber, List<String> skuList) {
        return getByOrderNumber(orderNumber).stream()
                .filter(dor -> skuList.contains(dor.getSku()))
                .filter(dor -> dor.getQuantityShipped() >= dor.getQuantity())
                .map(DropshipmentOrderRow::getSku)
                .collect(Collectors.toList());
    }
}
