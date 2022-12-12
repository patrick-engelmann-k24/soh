package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.salesOrderHub.services.financialdocuments.CreditNoteNumberCounterService;
import de.kfzteile24.salesOrderHub.services.returnorder.ReturnOrderServiceAdaptor;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesOrderReturnServiceTest {

    @Mock
    private SalesOrderReturnRepository salesOrderReturnRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private CreditNoteNumberCounterService creditNoteNumberCounterService;

    @Mock
    private CamundaHelper helper;

    @Mock
    private OrderUtil orderUtil;

    @Mock
    private ReturnOrderServiceAdaptor adaptor;


    @InjectMocks
    private SalesOrderReturnService salesOrderReturnService;

    @Test
    @SneakyThrows
    @DisplayName(("Create Credit Note Number When Latest Credit Note Number Is Empty"))
    public void testCreateCreditNoteNumberWhenLatestCreditNoteNumberIsEmpty() {
        var currentYear = 2022;
        when(creditNoteNumberCounterService.getNextCounter(currentYear)).thenReturn(1L);
        assertThat(salesOrderReturnService.createCreditNoteNumber(currentYear)).isEqualTo("2022200001");
    }

    @Test
    @SneakyThrows
    @DisplayName(("Create Credit Note Number When Latest Credit Note Number Is Not Empty"))
    public void testCreateCreditNoteNumberWhenLatestCreditNoteNumberIsNotEmpty() {
        var currentYear = 2022;
        when(creditNoteNumberCounterService.getNextCounter(currentYear)).thenReturn(2L);
        assertThat(salesOrderReturnService.createCreditNoteNumber(currentYear)).isEqualTo("2022200002");
    }

    @Test
    @SneakyThrows
    @DisplayName(("Test createCreditNoteEventMessage method"))
    public void testCreateCreditNoteEventMessage() {
        var order = getObjectByResource("ecpOrderMessage.json", Order.class);
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());
        var salesOrder =  getSalesOrder(order);
        var orderNumber = order.getOrderHeader().getOrderNumber();
        var orderGroupId = order.getOrderHeader().getOrderGroupId();
        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        message.getSalesCreditNote().getSalesCreditNoteHeader().setOrderNumber(orderNumber);
        message.getSalesCreditNote().getSalesCreditNoteHeader().setOrderGroupId(null);
        var salesCreditNoteCreatedMessage = salesOrderReturnService.createCreditNoteEventMessage(salesOrder.getOrderGroupId(), message, orderNumber);
        assertEquals(orderGroupId, salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderGroupId());
    }
}
