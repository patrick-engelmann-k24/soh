package de.kfzteile24.salesOrderHub.controller;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderAddress;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.dto.sqs.EcpOrder;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderItemService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    CamundaHelper camundaHelper;

    @Autowired
    private SalesOrderItemService orderItemService;

    @GetMapping("/{orderNumber}/isChangeable")
    public Boolean orderIsChangeable(@PathVariable String orderNumber) {
        return false;
    }

    @PutMapping("/{orderNumber}/billingAdresss")
    public Address getBillingAddress(@PathVariable String orderNumber) {
        // todo implement invoice change accordingly to deliveryAddress change
        return null;
    }

    @PutMapping("/{orderNumber}/{orderItemId}/deliveryAddress")
    @ResponseStatus(HttpStatus.OK)
    public Address updateBillingAddress(
            @PathVariable("orderNumber") final String orderNumber, @PathVariable("orderItemId") final String orderItemId, @RequestBody final Address address) {
        return orderItemService.changeDeliveryAddress(orderNumber, orderItemId, address);
    }

    @GetMapping("/{orderNumber}")
    public OrderJSON getOrder(@PathVariable String orderNumber) {
        final Optional<SalesOrder> salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        if (salesOrder.isEmpty()) {
            return null;
        }

        return salesOrder.get().getOriginalOrder();
    }
}
