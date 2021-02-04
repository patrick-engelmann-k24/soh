package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.services.SalesOrderAddressService;
import de.kfzteile24.salesOrderHub.services.SalesOrderItemService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SalesOrderAddressService orderAddressService;

    @Autowired
    private SalesOrderItemService orderItemService;

    @PutMapping("/{orderNumber}/billingAdresss")
    public ResponseEntity<Address> updateBillingAddress(@PathVariable String orderNumber, @RequestBody final Address address) {
        return orderAddressService.updateBillingAddress(orderNumber, address);
    }

    @PutMapping("/{orderNumber}/{orderItemId}/deliveryAddress")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Address> updateDeliveryAddress(
            @PathVariable("orderNumber") final String orderNumber, @PathVariable("orderItemId") final String orderItemId, @RequestBody final Address address) {
        return orderAddressService.updateDeliveryAddress(orderNumber, orderItemId, address);
    }

    @PutMapping("/{orderNumber}/cancel")
    @ResponseBody
    public ResponseEntity cancelOrder(
            @PathVariable("orderNumber") final String orderNumber
    ) {
        //ToDo hier weiter machen
        salesOrderService.cancelOrder(orderNumber);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{orderNumber}/cancelItem/{orderItemId}")
    @ResponseBody
    public ResponseEntity cancelOrderItem(
            @PathVariable("orderNumber") final String orderNumber,
            @PathVariable("orderItemId") final String orderItemId
    ) {
        orderItemService.cancelOrderItem(orderNumber, orderItemId);
        return ResponseEntity.ok().build();
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
