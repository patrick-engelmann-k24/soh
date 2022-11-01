package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.BillingAddress;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_ADDRESS_CHANGED;

@Service
@RequiredArgsConstructor
public class SalesOrderAddressService {

    private final SalesOrderService salesOrderService;

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final CamundaHelper helper;

    @NonNull
    private final SalesOrderRepository orderRepository;

    @NonNull
    private final RuntimeService runtimeService;

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final TimedPollingService timerService;

    @NonNull
    private final InvoiceService invoiceService;

    @SneakyThrows
    public ResponseEntity<String> updateBillingAddress(final String orderNumber, final BillingAddress newBillingAddress) {
        final Optional<SalesOrder> soOpt = orderRepository.getOrderByOrderNumber(orderNumber);
        if (soOpt.isPresent()) {
            if (invoiceService.checkInvoiceExistsForOrder(orderNumber)) {
                return new ResponseEntity<>("The billing address cannot be changed anymore, because the order has already an invoice.", HttpStatus.CONFLICT);
            } else {

                var salesOrder = soOpt.get();

                salesOrder.getLatestJson().getOrderHeader().setBillingAddress(newBillingAddress);
                salesOrderService.save(salesOrder, INVOICE_ADDRESS_CHANGED);
                snsPublishService.publishInvoiceAddressChanged(orderNumber);
                return new ResponseEntity<>("", HttpStatus.OK);
            }
        }

        return ResponseEntity.notFound().build();
    }
}
