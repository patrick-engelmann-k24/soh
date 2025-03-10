package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.helper.ReturnOrderHelper;
import de.kfzteile24.salesOrderHub.repositories.CreditNoteNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.salesOrderHub.services.financialdocuments.CreditNoteNumberCounterService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SalesOrderReturnServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderReturnRepository salesOrderReturnRepository;

    @Autowired
    private ReturnOrderHelper returnOrderHelper;

    @Autowired
    private CreditNoteNumberCounterRepository creditNoteNumberCounterRepository;

    @Autowired
    private CreditNoteNumberCounterService creditNoteNumberCounterService;

    @Test
    @SneakyThrows
    @DisplayName("Create First Credit Note Number, Then Create Sales Credit Note, Then Create Second Credit Note Number")
    void testCreateFirstCreditNoteNumberThenCreateSalesCreditNoteThenCreateSecondCreditNoteNumber() {
        String creditNoteNumber = salesOrderReturnService.createCreditNoteNumber();

        var orderMessage = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        orderMessage.getOrderHeader().setOrderNumber(RandomStringUtils.randomNumeric(9));
        orderMessage.getOrderHeader().setOrderFulfillment("delticom");
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(orderMessage));
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "123");

        var message = getObjectByResource("dropshipmentPurchaseOrderReturnConfirmed.json", DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        message.setSalesOrderNumber(salesOrder.getOrderNumber());
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage = returnOrderHelper.buildSalesCreditNoteCreatedMessage(message);
        salesOrderReturn.setSalesCreditNoteCreatedMessage(salesCreditNoteCreatedMessage);

        SalesOrderReturn updatedOrder = salesOrderReturnService.save(salesOrderReturn, RETURN_ORDER_CREATED);
        assertNotNull(updatedOrder);
        assertEquals(getNextCreditNoteNumber(creditNoteNumber), updatedOrder.getSalesCreditNoteCreatedMessage().getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber());
    }

    private static String getNextCreditNoteNumber(String creditNoteNumber) {
        var length = creditNoteNumber.length();
        return creditNoteNumber.substring(0, length - 5) + String.format("%05d", Integer.parseInt(creditNoteNumber.substring(length - 5, length)) + 1);
    }

    @BeforeEach
    public void prepare() {
        creditNoteNumberCounterRepository.deleteAll();
        creditNoteNumberCounterService.init();
    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        salesOrderRepository.deleteAll();
        salesOrderReturnRepository.deleteAll();
    }
}
