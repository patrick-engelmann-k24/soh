package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderInvoice;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class
)
public class InvoiceServiceIntegrationTest {
    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderInvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Test
    public void anInvoiceIsPersistedCorrectly() {
        final var testOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = testOrder.getOrderNumber();
        final var invoice = createSalesOrderInvoice(orderNumber, false);

        final var persistedSalesOrder = salesOrderRepository.save(testOrder);
        invoice.setSalesOrder(persistedSalesOrder);
        invoiceService.saveInvoice(invoice);

        final var persistedInvoices = invoiceService.getInvoicesByOrderNumber(orderNumber);
        assertThat(persistedInvoices.size()).isEqualTo(1);
        final var persistedInvoice = persistedInvoices.iterator().next();
        assertThat(persistedInvoice.getSalesOrder().getOrderNumber()).isEqualTo(orderNumber);
        assertThat(persistedInvoice.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(persistedInvoice.getInvoiceNumber()).isEqualTo(persistedInvoice.getInvoiceNumber());
        assertThat(persistedInvoice.getUrl()).isEqualTo(persistedInvoice.getUrl());
        assertThat(persistedInvoice.getCreatedAt()).isNotNull();
        assertThat(persistedInvoice.getUpdatedAt()).isNotNull();
    }

    @Test
    public void addingASalesOrderToExistingInvoicesUpdatesTheInvoiceTableCorrectly() {
        final var testOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = testOrder.getOrderNumber();
        final var existingInvoices = List.of(
                createSalesOrderInvoice(orderNumber, false),
                createSalesOrderInvoice(orderNumber, true));
        invoiceRepository.saveAll(existingInvoices);

        salesOrderRepository.save(testOrder);

        existingInvoices.forEach(invoice -> invoiceService.addSalesOrderToInvoice(testOrder, invoice));

        final var fetchedInvoices = invoiceService.getInvoicesByOrderNumber(orderNumber);
        assertThat(fetchedInvoices.size()).isEqualTo(2);
        existingInvoices.forEach(existingInvoice -> {
           final var foundInvoice = fetchedInvoices.stream()
                   .filter(fetchedInvoice -> fetchedInvoice.getInvoiceNumber().equals(existingInvoice.getInvoiceNumber()))
                   .findFirst()
                   .orElseThrow();
           assertThat(foundInvoice.getSalesOrder().getOrderNumber()).isEqualTo(orderNumber);
           assertThat(foundInvoice.getOrderNumber()).isEqualTo(orderNumber);
           assertThat(foundInvoice.getUrl()).isEqualTo(foundInvoice.getUrl());
        });
    }
}
