package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerMvcTest extends AbstractControllerMvcTest {

    @MockBean
    private SnsPublishService snsPublishService;

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
    void testRepublishOrdersFailedForSOmeOrders() throws Exception {

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
}
