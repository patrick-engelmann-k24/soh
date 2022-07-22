package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.soh.order.dto.BillingAddress;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.INVOICE_NUMBER_SEPARATOR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    @NonNull
    private final SalesOrderInvoiceRepository invoiceRepository;

    @NonNull
    private final AuditLogRepository auditLogRepository;

    /**
     * If we find an invoice, there are already invoice(s) created
     *
     * @param orderNumber
     * @return
     */
    public boolean checkInvoiceExistsForOrder(final String orderNumber) {
        return invoiceRepository.existsByOrderNumber(orderNumber);
    }

    public Set<SalesOrderInvoice> getInvoicesByOrderNumber(String orderNumber) {
        return invoiceRepository.getInvoicesByOrderNumber(orderNumber);
    }

    public SalesOrderInvoice saveInvoice(SalesOrderInvoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public SalesOrderInvoice addSalesOrderToInvoice(SalesOrder salesOrder, SalesOrderInvoice invoice) {
        final var auditLog = AuditLog.builder()
                .salesOrderId(invoice.getId())
                .action(INVOICE_RECEIVED)
                .data(salesOrder.getLatestJson())
                .createdAt(invoice.getCreatedAt())
                .build();

        auditLogRepository.save(auditLog);

        invoice.setSalesOrder(salesOrder);
        return saveInvoice(invoice);
    }

    public String createInvoiceNumber() {
        var currentYear = LocalDateTime.now().getYear();
        return currentYear + "-1" + getNextInvoiceCount(currentYear);
    }

    private String getNextInvoiceCount(int currentYear) {
        var found = invoiceRepository.findFirstBySourceAndInvoiceNumberStartsWithOrderByInvoiceNumberDesc(InvoiceSource.SOH, currentYear + "-1");
        if (found.isPresent()) {
            return createNextInvoiceNumberCounter(found.get());
        }
        return "000000000001";
    }

    private String createNextInvoiceNumberCounter(SalesOrderInvoice salesOrderInvoice) {
        var counter = Long.valueOf(extractInvoiceCounterNumber(salesOrderInvoice));
        counter++;
        return String.format("%012d", counter);
    }

    private String extractInvoiceCounterNumber(SalesOrderInvoice salesOrderInvoice) {
        if (!salesOrderInvoice.getInvoiceNumber().contains(INVOICE_NUMBER_SEPARATOR))
            throw new IllegalArgumentException("Last sales order invoice in DB does not contain '-' in the invoice number");
        String[] split = salesOrderInvoice.getInvoiceNumber().split(INVOICE_NUMBER_SEPARATOR);
        return split[split.length - 1].substring(1);
    }

    public CoreSalesInvoiceCreatedMessage generateInvoiceMessage(SalesOrder salesOrder) {
        List<CoreSalesFinancialDocumentLine> invoiceLines = new ArrayList<>();
        for (OrderRows row : salesOrder.getLatestJson().getOrderRows()) {
            invoiceLines.add(CoreSalesFinancialDocumentLine.builder()
                    .itemNumber(row.getSku())
                    .quantity(row.getQuantity())
                    .taxRate(row.getTaxRate())
                    .unitNetAmount(Optional.ofNullable(row.getUnitValues().getDiscountedNet()).orElse(BigDecimal.ZERO))
                    .lineNetAmount(Optional.ofNullable(row.getSumValues().getTotalDiscountedNet()).orElse(BigDecimal.ZERO))
                    .unitGrossAmount(Optional.ofNullable(row.getUnitValues().getDiscountedGross()).orElse(BigDecimal.ZERO))
                    .lineGrossAmount(Optional.ofNullable(row.getSumValues().getTotalDiscountedGross()).orElse(BigDecimal.ZERO))
                    .lineTaxAmount(Optional.ofNullable(row.getSumValues().getTotalDiscountedGross()).orElse(BigDecimal.ZERO)
                            .subtract(Optional.ofNullable(row.getSumValues().getTotalDiscountedNet()).orElse(BigDecimal.ZERO)))
                    .isShippingCost(false)
                    .build());
        }

        var orderHeader = salesOrder.getLatestJson().getOrderHeader();
        return CoreSalesInvoiceCreatedMessage.builder()
                .salesInvoice(new CoreSalesInvoice(
                        new CoreSalesInvoiceHeader(
                                orderHeader.getDocumentRefNumber(),
                                LocalDateTime.now(),
                                invoiceLines,
                                salesOrder.getOrderGroupId(),
                                salesOrder.getOrderNumber(),
                                orderHeader.getOrderCurrency(),
                                Optional.ofNullable(orderHeader.getTotals().getGrandTotalNet()).orElse(BigDecimal.ZERO).doubleValue(),
                                Optional.ofNullable(orderHeader.getTotals().getGrandTotalGross()).orElse(BigDecimal.ZERO).doubleValue(),
                                Address.builder()
                                        .salutation(orderHeader.getBillingAddress().getSalutation())
                                        .firstName(orderHeader.getBillingAddress().getFirstName())
                                        .lastName(orderHeader.getBillingAddress().getLastName())
                                        .street(getStreet(orderHeader.getBillingAddress()))
                                        .city(orderHeader.getBillingAddress().getCity())
                                        .zipCode(orderHeader.getBillingAddress().getZipCode())
                                        .countryCode(orderHeader.getBillingAddress().getCountryCode())
                                        .build()
                        ),
                        new ArrayList<>()))
                .build();
    }

    private String getStreet(BillingAddress address) {
        return Address.getStreet(address);
    }
}
