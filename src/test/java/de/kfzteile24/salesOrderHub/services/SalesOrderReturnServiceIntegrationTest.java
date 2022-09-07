package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.ReturnOrderHelper;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SalesOrderReturnServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderReturnRepository salesOrderReturnRepository;

    @Autowired
    private SalesOrderReturnService salesOrderReturnService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private ReturnOrderHelper returnOrderHelper;

    @Test
    @SneakyThrows
    @DisplayName("Create First Credit Note Number, Then Create Sales Credit Note, Then Create Second Credit Note Number")
    void testCreateFirstCreditNoteNumberThenCreateSalesCreditNoteThenCreateSecondCreditNoteNumber() {
        String nextCreditNoteNumber = salesOrderReturnService.createCreditNoteNumber();

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderNumber(RandomStringUtils.randomNumeric(9));
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "123");

        String rawMessage = readResource("examples/dropshipmentPurchaseOrderReturnConfirmed.json");
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentPurchaseOrderReturnConfirmedMessage message =
                objectMapper.readValue(body, DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        message.setSalesOrderNumber(salesOrder.getOrderNumber());
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage = returnOrderHelper.buildSalesCreditNoteCreatedMessage(
                message, salesOrder, nextCreditNoteNumber);
        salesOrderReturn.setSalesCreditNoteCreatedMessage(salesCreditNoteCreatedMessage);

        salesOrderReturnService.save(salesOrderReturn, RETURN_ORDER_CREATED);

        SalesOrderReturn updatedOrder = salesOrderReturnService.getByOrderNumber(salesOrderReturn.getOrderNumber());
        assertNotNull(updatedOrder);
        assertEquals(nextCreditNoteNumber, updatedOrder.getSalesCreditNoteCreatedMessage().getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber());

        String result = salesOrderReturnService.createCreditNoteNumber();
        assertTrue(result.endsWith("00002"));
    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        salesOrderRepository.deleteAll();
        salesOrderReturnRepository.deleteAll();
    }
}
