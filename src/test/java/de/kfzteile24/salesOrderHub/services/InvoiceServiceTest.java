package de.kfzteile24.salesOrderHub.services;


import com.amazonaws.services.s3.model.S3Object;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.services.export.AmazonS3Service;
import de.kfzteile24.soh.order.dto.BillingAddress;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.UnitValues;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;

import static de.kfzteile24.salesOrderHub.constants.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderInvoice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private SalesOrderInvoiceRepository invoiceRepository;

    @Mock
    private InvoiceNumberCounterService invoiceNumberCounterService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private OrderUtil orderUtil;


    @Mock
    private AmazonS3Service amazonS3Service;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void addingASalesOrderToAnInvoiceUpdatesTheInvoiceAndCreatesAnAuditLogEntry() {
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var invoice = createSalesOrderInvoice(salesOrder.getOrderNumber(), false);
        invoice.setCreatedAt(LocalDateTime.MIN);

        invoiceService.addSalesOrderToInvoice(salesOrder, invoice);

        final var expectedAuditLog = AuditLog.builder()
                .salesOrderId(invoice.getId())
                .action(INVOICE_RECEIVED)
                .data(salesOrder.getLatestJson())
                .createdAt(invoice.getCreatedAt())
                .build();

        verify(auditLogRepository).save(eq(expectedAuditLog));
        verify(invoiceRepository).save(argThat(inv -> {
            assertThat(inv.getSalesOrder()).isEqualTo(salesOrder);
            return true;
        }));
    }

    @Test
    void testGenerateInvoiceMessage() {
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber("invoice-number");
        for (OrderRows row : salesOrder.getLatestJson().getOrderRows()) {
            row.setUnitValues(UnitValues.builder()
                    .goodsValueGross(BigDecimal.valueOf(9))
                    .goodsValueNet(BigDecimal.valueOf(3))
                    .discountGross(BigDecimal.valueOf(3))
                    .discountNet(BigDecimal.valueOf(1))
                    .discountedGross(BigDecimal.valueOf(6))
                    .discountedNet(BigDecimal.valueOf(2))
                    .build());
        }
        salesOrder.getLatestJson().getOrderHeader().setBillingAddress(BillingAddress.builder()
                        .city("Berlin")
                        .street1("asdfgh")
                        .street2("jkl")
                        .zipCode("06200")
                        .countryCode("DE")
                        .firstName("first-name")
                        .lastName("last-name")
                        .salutation("abc")
                .build());
        var invoice = createSalesOrderInvoice(salesOrder.getOrderNumber(), false);
        invoice.setCreatedAt(LocalDateTime.MIN);

        var result = invoiceService.generateInvoiceMessage(salesOrder);

        assertThat(result.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()).isEqualTo("invoice-number");
        assertThat(result.getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo(salesOrder.getOrderNumber());
        assertThat(result.getSalesInvoice().getSalesInvoiceHeader().getInvoiceLines().size()).isEqualTo(3);

        var itemLine = result.getSalesInvoice().getSalesInvoiceHeader().getInvoiceLines().get(0);
        var itemNumber = itemLine.getItemNumber();
        var row = salesOrder.getLatestJson().getOrderRows().stream().filter(r -> r.getSku().equals(itemNumber)).findFirst().orElse(null);
        assertThat(row).isNotNull();
        assertThat(itemLine.getIsShippingCost()).isFalse();
        assertThat(itemLine.getUnitNetAmount()).isEqualTo(row.getUnitValues().getDiscountedNet());
        var totalDiscountedGross = row.getSumValues().getTotalDiscountedGross().doubleValue();
        var totalDiscountedNet = row.getSumValues().getTotalDiscountedNet().doubleValue();
        assertThat(itemLine.getLineTaxAmount()).isEqualTo(BigDecimal.valueOf(totalDiscountedGross - totalDiscountedNet).setScale(0));
        assertThat(result.getSalesInvoice().getSalesInvoiceHeader().getInvoiceDate()).isEqualTo(result.getSalesInvoice().getSalesInvoiceHeader().getInvoiceDate());
    }

    @Test
    void testGetInvoiceDocument() {
        var invoice = createSalesOrderInvoice("test", false);
        var bytes = new byte[] { 77, 97, 114, 121 };
        var inputStream =  new ByteArrayInputStream(bytes);
        var encodedContent = Base64.getEncoder().encodeToString(bytes);
        var s3Object = new S3Object();
        s3Object.setObjectContent(inputStream);
        s3Object.setKey(invoice.getInvoiceNumber());
        when(invoiceRepository.getInvoicesByInvoiceNumber(eq(invoice.getInvoiceNumber()))).thenReturn(Collections.singleton(invoice));
        when(amazonS3Service.downloadFile(eq(invoice.getUrl()))).thenReturn(s3Object);

        var result = invoiceService.getInvoiceDocument(invoice.getInvoiceNumber());

        assertThat(result.getInvoiceNumber()).isEqualTo(invoice.getInvoiceNumber());
        assertThat(result.getContent()).isEqualTo(encodedContent);
    }


}