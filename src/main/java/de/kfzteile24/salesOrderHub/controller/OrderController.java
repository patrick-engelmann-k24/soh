package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderAddressService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.BillingAddress;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.ShippingAddress;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Order Rest Controller Object
 *
 * <P>Rest-Endpoint for direct request to handle changes on an order or order row</P>
 *
 * @author Robert, Andreas
 * @version 1.0
 */
@Slf4j
@Api(value = "OrderController")
@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SalesOrderAddressService orderAddressService;

    /**
     * Change billing address if there no invoice exists
     *
     * @param orderNumber The order number from the order where to change the billing address
     * @return ResponseEntity with Address
     */
    @ApiOperation(value = "Change billing address if there no invoice exists")
    @PutMapping("/{orderNumber}/billingAddress")
    public ResponseEntity<String> updateBillingAddress(@PathVariable String orderNumber, @RequestBody final BillingAddress address) {
        return orderAddressService.updateBillingAddress(orderNumber, address);
    }

    /**
     * Change delivery address for the order row if this is not over an defined state
     *
     * <P>Parcel: only before "Packing started"<br/>
     * Own delivery: only before "Tour started"<br/>
     * Pickup: n/a</P>
     *
     * @param orderNumber The order number from the order where the order row is part of it
     * @param orderRowId The order row id where the address tried to changed
     * @param address The new delivery address
     * @return Response entity with the result of the delivery address update
     */
    @ApiOperation(value = "Change delivery address for the order row if this is not over an defined state")
    @PutMapping("/{orderNumber}/{orderItemId}/deliveryAddress")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> updateDeliveryAddress(
            @PathVariable("orderNumber") final String orderNumber, @PathVariable("orderItemId") final String orderRowId, @RequestBody final ShippingAddress address) {
        return orderAddressService.updateDeliveryAddress(orderNumber, orderRowId, address);
    }

    /**
     * Try to cancel complete order if it has not reached a defined state
     *
     * <p>The order can only cancelled if the order rows are not in the subprocess for fulfillment.</p>
     * <p>If the order rows are in the subprocess for fulfillment, you need to request order row cancel for each row.</p>
     *
     * @param orderNumber The order number from the order which should be cancelled
     * @return ResponseEntity with optional message if not possible
     */
    @ApiOperation(value = "Try to cancel complete order if it has not reached a defined state")
    @PutMapping("/{orderNumber}/cancel")
    @ResponseBody
    public ResponseEntity<String> cancelOrder(
            @PathVariable("orderNumber") final String orderNumber
    ) {
        log.info("Cancel order endpoint is not used anymore");
        return ResponseEntity.notFound().build();
    }

    /**
     * Try to cancel order row if it has not reached a defined state
     *
     * <P>
 *     Parcel: only before "TrackingID received"<br/>
     * Own delivery: only before "Item delivered"<br/>
     * Pickup: only before "Item picked up"<br/>
     * </P>
     *
     * @param orderNumber The order number where the order row is part of it
     * @param orderRowId The id of the order row which should tried to cancelled
     * @return ResponseEntity with optional message if not possible
     */
    @ApiOperation(value = "Try to cancel order row if it has not reached a defined state")
    @PutMapping("/{orderNumber}/cancelItem/{orderItemId}")
    @ResponseBody
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success|OK"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Failed|Not possible")
    })
    public ResponseEntity<String> cancelOrderRow(
            @PathVariable("orderNumber") final String orderNumber,
            @PathVariable("orderItemId") final String orderRowId
    ) {
        log.info("Cancel order row endpoint is not used anymore");
        return ResponseEntity.notFound().build();
    }

    /**
     * Get one order with order number
     * @param orderNumber The order number of the order which should be returned
     * @return OrderJson
     */
    @ApiOperation(value = "Get one order with order number")
    @GetMapping("/{orderNumber}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderNumber) {
        final Optional<SalesOrder> salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        if (salesOrder.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(salesOrder.get().getLatestJson());
    }
}
