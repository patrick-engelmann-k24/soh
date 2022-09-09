package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.dto.invoice.InvoiceDocument;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.salesOrderHub.exception.InvoiceDocumentNotFoundException;
import de.kfzteile24.salesOrderHub.exception.InvoiceNotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.services.export.AmazonS3Service;
import de.kfzteile24.soh.order.dto.BillingAddress;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.camunda.commons.utils.IoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    @NonNull
    private final SalesOrderInvoiceRepository invoiceRepository;

    @NonNull
    private final InvoiceNumberCounterService invoiceNumberCounterService;

    @NonNull
    private final AuditLogRepository auditLogRepository;

    @NonNull
    private final OrderUtil orderUtil;

    @NonNull
    private final AmazonS3Service amazonS3Service;

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

    @Transactional(readOnly = true)
    public InvoiceDocument getInvoiceDocument(String invoiceNumber) {
        var bytes = getInvoiceDocumentAsByteArray(invoiceNumber);
        var encodedContent = Base64.getEncoder().encodeToString(bytes);
        return InvoiceDocument.builder()
                .invoiceNumber(invoiceNumber)
                .content(encodedContent)
                .build();
    }

    @Transactional(readOnly = true)
    @SneakyThrows
    public byte[] getInvoiceDocumentAsByteArray(String invoiceNumber) {
        var invoice = invoiceRepository.getInvoicesByInvoiceNumber(invoiceNumber).stream()
                .findFirst().orElseThrow(() -> new InvoiceNotFoundException(invoiceNumber));
        var s3File = amazonS3Service.downloadFile(invoice.getUrl());
        if (s3File == null || s3File.getObjectContent() == null) {
            throw new InvoiceDocumentNotFoundException(invoiceNumber, invoice.getUrl());
        }
        return IoUtil.inputStreamAsByteArray(s3File.getObjectContent());
    }

    public String createInvoiceNumber() {
        var currentYear = LocalDateTime.now().getYear();
        return currentYear + "-1" + getNextInvoiceCount(currentYear);
    }

    private String getNextInvoiceCount(int currentYear) {
        Long invoiceNumber = invoiceNumberCounterService.getNextCounter(currentYear);
        return String.format("%012d", invoiceNumber);
    }

    public CoreSalesInvoiceCreatedMessage generateInvoiceMessage(SalesOrder salesOrder) {
        List<CoreSalesFinancialDocumentLine> invoiceLines = new ArrayList<>();
        for (OrderRows row : salesOrder.getLatestJson().getOrderRows()) {
            invoiceLines.add(CoreSalesFinancialDocumentLine.builder()
                    .itemNumber(row.getSku())
                    .description(row.getName())
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

        if (orderUtil.hasShippingCost(salesOrder)) {
            invoiceLines.add(orderUtil.createShippingCostLineFromSalesOrder(salesOrder));
        }

        var orderHeader = salesOrder.getLatestJson().getOrderHeader();
        return CoreSalesInvoiceCreatedMessage.builder()
                .salesInvoice(new CoreSalesInvoice(
                        new CoreSalesInvoiceHeader(
                                orderHeader.getDocumentRefNumber(),
                                Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()),
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
