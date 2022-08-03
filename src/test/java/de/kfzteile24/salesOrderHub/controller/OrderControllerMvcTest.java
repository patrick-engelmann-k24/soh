package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;

import static de.kfzteile24.salesOrderHub.controller.dto.ActionType.RECREATE_ORDER_INVOICE;
import static de.kfzteile24.salesOrderHub.controller.dto.ActionType.REPUBLISH_ORDER;
import static de.kfzteile24.salesOrderHub.controller.dto.ActionType.REPUBLISH_RETURN_ORDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerMvcTest extends AbstractControllerMvcTest {

    public static final String ORDER_NUMBER_1 = "2345235";
    public static final String ORDER_NUMBER_2 = "3454235";
    public static final String API_V_1_ORDER_APPLY_ACTION = "/api/v1/order/apply-action";
    public static final String QUERY_PARAM_ACTION_TYPE = "actionType";

    @MockBean
    private SnsPublishService snsPublishService;

    @MockBean
    private DropshipmentOrderService dropshipmentOrderService;

    @Test
    void testRepublishOrders() throws Exception {

        var orderNumbers = List.of("2345235", "3454235");
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk());

        verify(snsPublishService).publishMigrationOrderCreated("2345235");
        verify(snsPublishService).publishMigrationOrderCreated("3454235");
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

        var orderNumbers = List.of("2345235", "3454235");
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        doThrow(new SalesOrderNotFoundException("3454235")).when(snsPublishService)
                .publishMigrationOrderCreated("3454235");

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].order_number").value("3454235"))
                .andExpect(jsonPath("$[0].error_message").value("Sales order not found for the given order number 3454235 "));

        verify(snsPublishService).publishMigrationOrderCreated("2345235");
        verify(snsPublishService).publishMigrationOrderCreated("3454235");
    }

    @Test
    void testRecreateInvoice() throws Exception {

        var orderNumbers = List.of(ORDER_NUMBER_1, ORDER_NUMBER_2);
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        var salesOrder1 = SalesOrder.builder()
                .orderNumber(ORDER_NUMBER_1)
                .build();
        var salesOrder2 = SalesOrder.builder()
                .orderNumber(ORDER_NUMBER_2)
                .build();

        when(dropshipmentOrderService.recreateSalesOrderInvoice(ORDER_NUMBER_1)).thenReturn(salesOrder1);
        when(dropshipmentOrderService.recreateSalesOrderInvoice(ORDER_NUMBER_2)).thenReturn(salesOrder2);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, RECREATE_ORDER_INVOICE.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk());

        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_1);
        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_2);
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

        when(dropshipmentOrderService.recreateSalesOrderInvoice(ORDER_NUMBER_1)).thenReturn(salesOrder1);

        doThrow(new SalesOrderNotFoundException(ORDER_NUMBER_2)).when(dropshipmentOrderService)
                .recreateSalesOrderInvoice(ORDER_NUMBER_2);

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
        verify(snsPublishService).publishCoreInvoiceReceivedEvent(eq(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder1.getInvoiceEvent())));
    }

    @Test
    void testRepublishReturnOrders() throws Exception {

        var orderNumbers = List.of("2345235", "3454235");
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_RETURN_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk());

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent("2345235");
        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent("3454235");
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

        var orderNumbers = List.of("2345235", "3454235");
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        doThrow(new SalesOrderNotFoundException("3454235")).when(snsPublishService)
                .publishMigrationReturnOrderCreatedEvent("3454235");

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, REPUBLISH_RETURN_ORDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].order_number").value("3454235"))
                .andExpect(jsonPath("$[0].error_message").value("Sales order not found for the given order number 3454235 "));

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent("2345235");
        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent("3454235");
    }

    @Test
    void testRepublishReturnOrdersEmptyOrderNumberWithEmptyActionType() throws Exception {

        var orderNumbers = List.of("2345235", "3454235");
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

        var orderNumbers = List.of("2345235", "3454235");
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post(API_V_1_ORDER_APPLY_ACTION)
                .param(QUERY_PARAM_ACTION_TYPE, "null")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(snsPublishService, never()).publishMigrationReturnOrderCreatedEvent(anyString());
    }
}
