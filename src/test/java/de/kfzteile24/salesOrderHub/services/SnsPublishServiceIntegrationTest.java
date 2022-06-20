package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
public class SnsPublishServiceIntegrationTest {

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
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "123");
        salesOrderReturnService.save(salesOrderReturn, RETURN_ORDER_CREATED);
        var salesCreditNoteCreatedEvent = creditNoteService.buildSalesCreditNoteCreatedEvent(salesOrderReturn.getOrderNumber(), "test");
        try {
            snsPublishService.publishCreditNoteCreatedEvent(salesCreditNoteCreatedEvent);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @AfterEach
    public void cleanup() {
        salesOrderReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
    }
}
