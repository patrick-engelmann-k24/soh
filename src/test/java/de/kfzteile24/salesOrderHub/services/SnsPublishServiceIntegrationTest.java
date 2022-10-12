package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.salesOrderHub.services.financialdocuments.CreditNoteService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;

class SnsPublishServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SnsPublishService snsPublishService;

    @Autowired
    private CreditNoteService creditNoteService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SalesOrderReturnService salesOrderReturnService;

    @Autowired
    private SalesOrderReturnRepository salesOrderReturnRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Test
    @SneakyThrows(Exception.class)
    @DisplayName("When Sales Credit Note Created Event Published Then Topic Is Found And No Exceptions Are Thrown")
    void whenSalesCreditNoteCreatedEventPublishedThenTopicIsFoundAndNoExceptionsAreThrown() {
        Order order = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "123");
        salesOrderReturnService.save(salesOrderReturn, RETURN_ORDER_CREATED);
        var salesCreditNoteCreatedEvent = creditNoteService.buildSalesCreditNoteCreatedEvent(salesOrderReturn.getOrderNumber(), "test");
        try {
            snsPublishService.publishCreditNoteCreatedEvent(salesCreditNoteCreatedEvent);
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @AfterEach
    public void cleanup() {
        salesOrderReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
    }
}
