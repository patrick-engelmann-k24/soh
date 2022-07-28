package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerMvcTest extends AbstractControllerMvcTest {

    public static final String ORDER_NUMBER_1 = "2345235";
    public static final String ORDER_NUMBER_2 = "3454235";
    @MockBean
    private SnsPublishService snsPublishService;

    @MockBean
    private DropshipmentOrderService dropshipmentOrderService;

    @Test
    void testRepublishOrders() throws Exception {

        var orderNumbers = List.of("2345235", "3454235");
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post("/api/v1/order/republish")
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

        mvc.perform(post("/api/v1/order/republish")
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

        mvc.perform(post("/api/v1/order/republish")
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

        mvc.perform(post("/api/v1/order/republish")
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

        mvc.perform(post("/api/v1/order/invoice/recreate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk());

        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_1);
        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_2);
        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(argThat(salesOrder ->
                StringUtils.equals(salesOrder.getOrderNumber(), ORDER_NUMBER_1)), eq(emptyList()));
        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(argThat(salesOrder ->
                StringUtils.equals(salesOrder.getOrderNumber(), ORDER_NUMBER_2)), eq(emptyList()));
    }

    @Test
    void testRecreateInvoiceEmptyOrderNumberList() throws Exception {

        var orderNumbers = List.of();
        var requestBody = objectMapper.writeValueAsString(orderNumbers);

        mvc.perform(post("/api/v1/order/invoice/recreate")
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

        mvc.perform(post("/api/v1/order/invoice/recreate")
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

        mvc.perform(post("/api/v1/order/invoice/recreate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].order_number").value(ORDER_NUMBER_2))
                .andExpect(jsonPath("$[0].error_message").value("Sales order not found for the given order number 3454235 "));

        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_1);
        verify(dropshipmentOrderService).recreateSalesOrderInvoice(ORDER_NUMBER_2);
        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(argThat(salesOrder ->
                StringUtils.equals(salesOrder.getOrderNumber(), ORDER_NUMBER_1)), eq(emptyList()));
        verify(snsPublishService, never()).publishSalesOrderShipmentConfirmedEvent(argThat(salesOrder ->
                StringUtils.equals(salesOrder.getOrderNumber(), ORDER_NUMBER_2)), eq(emptyList()));
    }
}
