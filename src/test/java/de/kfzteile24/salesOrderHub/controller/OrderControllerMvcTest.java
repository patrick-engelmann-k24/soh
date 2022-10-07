package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.controller.dto.ActionType.RECREATE_ORDER_INVOICE;
import static de.kfzteile24.salesOrderHub.controller.dto.ActionType.REPUBLISH_ORDER;
import static de.kfzteile24.salesOrderHub.controller.dto.ActionType.REPUBLISH_ORDER_INVOICE;
import static de.kfzteile24.salesOrderHub.controller.dto.ActionType.REPUBLISH_RETURN_ORDER;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerMvcTest extends AbstractIntegrationTest {

    public static final String ORDER_NUMBER_1 = "2345235";
    public static final String ORDER_NUMBER_2 = "3454235";
    public static final String API_V_1_ORDER_APPLY_ACTION = "/api/v1/order/apply-action";
    public static final String QUERY_PARAM_ACTION_TYPE = "actionType";

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Test
    void testRepublishOrders() throws Exception {

        var orderNumbers = List.of(ORDER_NUMBER_1, ORDER_NUMBER_2);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        doNothing().when(snsPublishService).publishMigrationOrderCreated(ORDER_NUMBER_1);
        doNothing().when(snsPublishService).publishMigrationOrderCreated(ORDER_NUMBER_2);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("RequestID"));

        verify(snsPublishService).publishMigrationOrderCreated(ORDER_NUMBER_1);
        verify(snsPublishService).publishMigrationOrderCreated(ORDER_NUMBER_2);
    }

    @Test
    void testRepublishOrdersEmptyOrderNumberList() throws Exception {

        var orderNumbers = List.of();
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(snsPublishService, never()).publishMigrationOrderCreated(anyString());
    }

    @Test
    void testRepublishOrdersEmptyOrderNumberWithinList() throws Exception {

        var orderNumbers = List.of("");
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(snsPublishService, never()).publishMigrationOrderCreated(anyString());
    }

    @Test
    void testRepublishOrdersFailedForSomeOrders() throws Exception {

        var orderNumbers = List.of(ORDER_NUMBER_1, ORDER_NUMBER_2);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        doNothing().when(snsPublishService).publishMigrationOrderCreated(ORDER_NUMBER_1);
        doThrow(new SalesOrderNotFoundException(ORDER_NUMBER_2)).when(snsPublishService)
                .publishMigrationOrderCreated(ORDER_NUMBER_2);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].order_number").value("3454235"))
                .andExpect(jsonPath("$[0].error_message").value("Sales order not found for the given order number 3454235 "));

        verify(snsPublishService).publishMigrationOrderCreated(ORDER_NUMBER_1);
        verify(snsPublishService).publishMigrationOrderCreated(ORDER_NUMBER_2);
    }

    @Test
    void testRecreateInvoice() throws Exception {

        var orderNumbers = List.of(ORDER_NUMBER_1, ORDER_NUMBER_2);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        var salesOrder1 = SalesOrder.builder()
                .orderNumber(ORDER_NUMBER_1)
                .invoiceEvent(CoreSalesInvoiceCreatedMessage.builder().build())
                .build();
        var salesOrder2 = SalesOrder.builder()
                .orderNumber(ORDER_NUMBER_2)
                .invoiceEvent(CoreSalesInvoiceCreatedMessage.builder().build())
                .build();
        reset(dropshipmentOrderService);

        doReturn(salesOrder1).when(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_1);
        doReturn(salesOrder2).when(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_2);
        doNothing().when(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder1, emptyList());
        doNothing().when(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder2, emptyList());
        doNothing().when(snsPublishService).publishCoreInvoiceReceivedEvent(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder1.getInvoiceEvent()));
        doNothing().when(snsPublishService).publishCoreInvoiceReceivedEvent(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder2.getInvoiceEvent()));

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, RECREATE_ORDER_INVOICE.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("RequestID"));

        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_1);
        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_2);
        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder1, emptyList());
        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder2, emptyList());
        verify(snsPublishService, times(2)).publishCoreInvoiceReceivedEvent(eq(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder1.getInvoiceEvent())));
    }

    @Test
    void testRecreateInvoiceEmptyOrderNumberList() throws Exception {

        var orderNumbers = List.of();
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, RECREATE_ORDER_INVOICE.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(dropshipmentOrderService, never()).recreateSalesOrderInvoice(anyString());
        verify(snsPublishService, never()).publishSalesOrderShipmentConfirmedEvent(any(), any());
    }

    @Test
    void testRecreateInvoiceEmptyOrderNumberWithinList() throws Exception {

        var orderNumbers = List.of(StringUtils.EMPTY);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, RECREATE_ORDER_INVOICE.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(dropshipmentOrderService, never()).recreateSalesOrderInvoice(anyString());
        verify(snsPublishService, never()).publishSalesOrderShipmentConfirmedEvent(any(), any());
    }

    @Test
    void testRecreateInvoiceFailedForSomeOrders() throws Exception {

        var orderNumbers = List.of(ORDER_NUMBER_1, ORDER_NUMBER_2);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        var salesOrder1 = SalesOrder.builder()
                .orderNumber(ORDER_NUMBER_1)
                .build();

        doReturn(salesOrder1).when(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_1);
        doThrow(new SalesOrderNotFoundException(ORDER_NUMBER_2)).when(dropshipmentOrderService)
                .recreateSalesOrderInvoice(ORDER_NUMBER_2);
        doNothing().when(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder1, emptyList());
        doNothing().when(snsPublishService).publishCoreInvoiceReceivedEvent(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder1.getInvoiceEvent()));

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, RECREATE_ORDER_INVOICE.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].order_number").value(ORDER_NUMBER_2))
                .andExpect(jsonPath("$[0].error_message").value("Sales order not found for the given order number 3454235 "));

        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_1);
        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_2);
        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder1, emptyList());
        verify(snsPublishService).publishCoreInvoiceReceivedEvent(eq(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder1.getInvoiceEvent())));
    }

    @Test
    void testRepublishReturnOrders() throws Exception {

        var orderNumbers = List.of(ORDER_NUMBER_1, ORDER_NUMBER_2);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        doNothing().when(snsPublishService).publishMigrationReturnOrderCreatedEvent(ORDER_NUMBER_1);
        doNothing().when(snsPublishService).publishMigrationReturnOrderCreatedEvent(ORDER_NUMBER_2);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_RETURN_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("RequestID"));

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(ORDER_NUMBER_1);
        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(ORDER_NUMBER_2);
    }

    @Test
    void testRepublishReturnOrdersEmptyOrderNumberList() throws Exception {

        var orderNumbers = List.of();
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_RETURN_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(snsPublishService, never()).publishMigrationReturnOrderCreatedEvent(anyString());
    }

    @Test
    void testRepublishReturnOrdersEmptyOrderNumberWithinList() throws Exception {

        var orderNumbers = List.of("");
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_RETURN_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(snsPublishService, never()).publishMigrationReturnOrderCreatedEvent(anyString());
    }

    @Test
    void testRepublishReturnOrdersFailedForSomeOrders() throws Exception {

        var orderNumbers = List.of(ORDER_NUMBER_1, ORDER_NUMBER_2);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        doNothing().when(snsPublishService).publishMigrationReturnOrderCreatedEvent(ORDER_NUMBER_1);
        doThrow(new SalesOrderNotFoundException(ORDER_NUMBER_2)).when(snsPublishService)
                .publishMigrationReturnOrderCreatedEvent(ORDER_NUMBER_2);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_RETURN_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].order_number").value("3454235"))
                .andExpect(jsonPath("$[0].error_message").value("Sales order not found for the given order number 3454235 "));

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(ORDER_NUMBER_1);
        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(ORDER_NUMBER_2);
    }

    @Test
    void testRepublishReturnOrdersEmptyOrderNumberWithEmptyActionType() throws Exception {

        var orderNumbers = List.of(ORDER_NUMBER_1, ORDER_NUMBER_2);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, StringUtils.EMPTY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(snsPublishService, never()).publishMigrationReturnOrderCreatedEvent(anyString());
    }

    @Test
    void testRepublishReturnOrdersEmptyOrderNumberWithActionTypeIsNull() throws Exception {

        var orderNumbers = List.of(ORDER_NUMBER_1, ORDER_NUMBER_2);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, "null")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(snsPublishService, never()).publishMigrationReturnOrderCreatedEvent(anyString());
    }

    @Test
    void testRepublishInvoices() throws Exception {

        CoreSalesInvoiceCreatedMessage invoiceCreatedMsg = salesOrderUtil.createInvoiceCreatedMsg(ORDER_NUMBER_1);
        SalesOrder salesOrder = SalesOrder.builder().invoiceEvent(invoiceCreatedMsg).build();

        var orderNumbers = List.of(ORDER_NUMBER_1);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        doNothing().when(snsPublishService).publishCoreInvoiceReceivedEvent(any());
        when(salesOrderService.getOrderByOrderNumber(eq(ORDER_NUMBER_1))).thenReturn(Optional.of(salesOrder));

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                        .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_ORDER_INVOICE.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("RequestID"));

        verify(snsPublishService).publishCoreInvoiceReceivedEvent(any());
    }
}
