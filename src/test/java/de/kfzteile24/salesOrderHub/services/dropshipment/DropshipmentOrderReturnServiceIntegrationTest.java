package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_ORDER_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Slf4j
class DropshipmentOrderReturnServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private DropshipmentOrderReturnService dropshipmentOrderReturnService;
    @Autowired
    private TimedPollingService timedPollingService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @BeforeEach
    public void setup() {
        super.setUp();
        timedPollingService.retry(() -> salesOrderRepository.deleteAllInBatch());
    }

    @Test
    void testHandleCreditNoteFromDropshipmentOrderReturn() {

        var orderMessage = getObjectByResource("ecpOrderMessage.json", Order.class);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(orderMessage));
        Order order = salesOrder.getLatestJson();
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "123");

        salesOrderReturnService.save(salesOrderReturn, RETURN_ORDER_CREATED);

        SalesOrderReturn updatedOrder = salesOrderReturnService.getByOrderNumber(salesOrderReturn.getOrderNumber())
                .orElseThrow(() -> new SalesOrderReturnNotFoundException(salesOrderReturn.getOrderNumber()));

        String message = "s3://production-k24-invoices/www-kfzteile24-de/2022/10/07/"
                + updatedOrder.getOrderNumber() + ".pdf";
        var messageWrapper = MessageWrapper.builder().build();

        dropshipmentOrderReturnService.handleCreditNoteFromDropshipmentOrderReturn(message, messageWrapper);

        verify(insights).recordCustomEvent(eq(DROPSHIPMENT_ORDER_CREDIT_NOTE_CREATED.getName()), argThat(eventAttributes -> {
                    assertThat(eventAttributes.get("OrderNumber")).isEqualTo(salesOrder.getOrderNumber());
                    assertThat(eventAttributes.get("OrderGroupId")).isEqualTo(salesOrder.getOrderGroupId());
                    assertThat(eventAttributes.get("Platform")).isEqualTo(order.getOrderHeader().getPlatform().name());
                    assertThat(eventAttributes.get("SalesChannel")).isEqualTo(order.getOrderHeader().getSalesChannel());
                    assertThat(eventAttributes.get("TotalGrossAmount")).isEqualTo(order.getOrderHeader().getTotals().getGrandTotalGross());
                    assertThat(eventAttributes.get("TotalNetAmount")).isEqualTo(order.getOrderHeader().getTotals().getGrandTotalNet());
                    return true;
                }
        ));
    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        timedPollingService.retry(() -> salesOrderRepository.deleteAllInBatch());
    }
}
