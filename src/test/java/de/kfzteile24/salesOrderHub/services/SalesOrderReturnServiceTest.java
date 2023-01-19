package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.DropShipmentConfig;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.helper.ObjectUtil;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.salesOrderHub.services.financialdocuments.CreditNoteNumberCounterService;
import de.kfzteile24.salesOrderHub.services.returnorder.ReturnOrderServiceAdaptor;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.extension.mockito.process.ProcessInstanceFake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.SalesOrderType.DROPSHIPMENT;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CamundaHelper camundaHelper;

    @Spy
    private OrderUtil orderUtil = new OrderUtil(new DropShipmentConfig(), new ObjectUtil(new ObjectMapper()));

    @Mock
    private ReturnOrderServiceAdaptor adaptor;


    @InjectMocks
    private SalesOrderReturnService salesOrderReturnService;

    @Test
    @SneakyThrows
    @DisplayName(("Create Credit Note Number When Latest Credit Note Number Is Empty"))
    void testCreateCreditNoteNumberWhenLatestCreditNoteNumberIsEmpty() {
        var currentYear = 2022;
        when(creditNoteNumberCounterService.getNextCounter(currentYear)).thenReturn(1L);
        assertThat(salesOrderReturnService.createCreditNoteNumber(currentYear)).isEqualTo("2022200001");
    }

    @Test
    @SneakyThrows
    @DisplayName(("Create Credit Note Number When Latest Credit Note Number Is Not Empty"))
    void testCreateCreditNoteNumberWhenLatestCreditNoteNumberIsNotEmpty() {
        var currentYear = 2022;
        when(creditNoteNumberCounterService.getNextCounter(currentYear)).thenReturn(2L);
        assertThat(salesOrderReturnService.createCreditNoteNumber(currentYear)).isEqualTo("2022200002");
    }

    @Test
    @SneakyThrows
    @DisplayName(("Test createCreditNoteEventMessage method"))
    void testCreateCreditNoteEventMessage() {
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

    @Test
    @SneakyThrows
    @DisplayName(("Test handleSalesOrderReturn method for dropshipment order"))
    void testHandleSalesOrderReturnForDropshipmentOrder() {
        var order1 = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        order1.getOrderHeader().setOrderGroupId(order1.getOrderHeader().getOrderNumber());
        order1.getOrderRows().get(0).setSku("sku-1");
        order1.getOrderRows().get(1).setSku("sku-2");
        var order2 = getObjectByResource("ecpOrderMessage.json", Order.class);
        order2.getOrderHeader().setOrderGroupId(order1.getOrderHeader().getOrderNumber());
        order2.getOrderHeader().setOrderNumber(order1.getOrderHeader().getOrderNumber() + "-1");
        order2.getOrderRows().get(0).setSku("new-sku");
        var salesOrder1 =  getSalesOrder(order1);
        salesOrder1.setCreatedAt(now());
        var salesOrder2 =  getSalesOrder(order2);
        salesOrder2.setCreatedAt(now().plusMinutes(1));
        var orderNumber = order1.getOrderHeader().getOrderNumber();
        var orderGroupId = order1.getOrderHeader().getOrderGroupId();
        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        message.getSalesCreditNote().getSalesCreditNoteHeader().setOrderNumber(orderNumber);
        message.getSalesCreditNote().getSalesCreditNoteHeader().setOrderGroupId(null);
        when(adaptor.getSalesOrderList(orderGroupId, DROPSHIPMENT)).thenReturn(List.of(salesOrder2, salesOrder1));
        when(salesOrderService.getOrderByOrderNumber(orderGroupId)).thenReturn(Optional.of(salesOrder1));
        when(salesOrderReturnRepository.findByOrderNumber(any())).thenReturn(Optional.empty());
        when(salesOrderReturnRepository.save(any())).thenAnswer((Answer<SalesOrderReturn>) invocation -> invocation.getArgument(0));
        doReturn(null).when(auditLogRepository).save(any());
        when(camundaHelper.correlateMessage(any(), anyString(), any()).getProcessInstance())
                .thenReturn(ProcessInstanceFake.builder().build());

        salesOrderReturnService.setPublishDelay(StringUtils.EMPTY);
        salesOrderReturnService.handleSalesOrderReturn(message, DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED, DROPSHIPMENT_ORDER_RETURN_CONFIRMED);

        verify(salesOrderReturnRepository).save(argThat(
                expected -> {
                    assertThat(expected.getSalesCreditNoteCreatedMessage().getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber()).isEqualTo("RO-876130");
                    assertThat(expected.getReturnOrderJson().getOrderHeader().getOrderNumber()).isEqualTo("RO-876130");
                    assertThat(expected.getReturnOrderJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(message.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber());
                    assertThat(expected.getReturnOrderJson().getOrderHeader().getTotals().getGoodsTotalGross()).isEqualTo(BigDecimal.valueOf(-31.03));
                    assertThat(expected.getReturnOrderJson().getOrderHeader().getTotals().getGoodsTotalNet()).isEqualTo(BigDecimal.valueOf(-25.78));
                    assertThat(expected.getReturnOrderJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.valueOf(-11.90).setScale(2, HALF_UP));
                    assertThat(expected.getReturnOrderJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.valueOf(-10.00).setScale(2, HALF_UP));
                    assertThat(expected.getReturnOrderJson().getOrderRows()).hasSize(3);
                    assertThat(expected.getReturnOrderJson().getOrderRows().stream().allMatch(row -> List.of("sku-1", "sku-2", "new-sku").contains(row.getSku()))).isTrue();
                    return true;
                }
        ));
    }

    @Test
    @SneakyThrows
    @DisplayName(("Test getOrderGroupId method"))
    void testGetOrderGroupId() {
        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        message.getSalesCreditNote().getSalesCreditNoteHeader().setOrderGroupId("123456789");
        var orderGroupId = salesOrderReturnService.getOrderGroupId(message);
        assertEquals("123456789", orderGroupId);
    }

    @Test
    @SneakyThrows
    @DisplayName(("Test getOrderGroupId method when orderGroupId is null"))
    void testGetOrderGroupIdWhenOrderGroupIdIsNull() {
        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        message.getSalesCreditNote().getSalesCreditNoteHeader().setOrderGroupId(null);
        var orderGroupId = salesOrderReturnService.getOrderGroupId(message);
        assertEquals(message.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber(), orderGroupId);
        assertEquals(orderGroupId, message.getSalesCreditNote().getSalesCreditNoteHeader().getOrderGroupId());
    }
}
