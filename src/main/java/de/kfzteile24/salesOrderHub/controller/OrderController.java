package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.controller.dto.ErrorResponse;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.SalesOrderAddressService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.soh.order.dto.BillingAddress;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.ShippingAddress;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * Order Rest Controller Object
 *
 * <P>Rest-Endpoint for direct request to handle changes on an order or order row</P>
 *
 * @author Robert, Andreas
 * @version 1.0
 */
@Slf4j
@Tag(name = "Sales order processing")
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final SalesOrderService salesOrderService;
    private final SalesOrderAddressService orderAddressService;
    private final SnsPublishService snsPublishService;
    private final DropshipmentOrderService dropshipmentOrderService;

    /**
     * Change billing address if there no invoice exists
     *
     * @param orderNumber The order number from the order where to change the billing address
     * @return ResponseEntity with Address
     */
    @Operation(summary = "Change billing address if there no invoice exists", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "orderNumber",
                    description = "The order number from the order where to change the billing address", example = "91345435")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Billing address updated successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})
    })
    @PutMapping("/{orderNumber}/billingAddress")
    @ResponseStatus(HttpStatus.OK)
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
    @Operation(summary = "Change delivery address for the order row if this is not over an defined state",
            parameters = {
                @Parameter(in = ParameterIn.PATH, name = "orderNumber",
                        description = "The order number from the order where the order row is part of it", example = "91345435"),
                @Parameter(in = ParameterIn.PATH, name = "orderRowId",
                        description = "The order row id where the address tried to changed", example = "11111")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Delivery address updated successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})
    })
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
    @Hidden
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
    @Hidden
    @PutMapping("/{orderNumber}/cancelItem/{orderItemId}")
    @ResponseBody
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
    @Operation(summary = "Get one order with order number", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "orderNumber",
                    description = "The order number of the order which should be returned", example = "91345435")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Sales order based on order number", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))}),
            @ApiResponse(responseCode  = "404", description  = "No sales order found")
    })
    @GetMapping("/{orderNumber}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderNumber) {
        final Optional<SalesOrder> salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        if (salesOrder.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(salesOrder.get().getLatestJson());
    }

    @Operation(summary = "Republish sales orders by a list of order numbers")
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Sales orders republished successfully"),
            @ApiResponse(responseCode  = "400", description  = "Republishing some of sales orders failed", content = {
                    @Content(mediaType = "application/json", array =
                    @ArraySchema(schema = @Schema(implementation = ErrorResponse.class)))}),
            @ApiResponse(responseCode  = "400", description  = "Invalid request", content = {
                    @Content(mediaType = "application/json", array =
                    @ArraySchema(schema = @Schema(implementation = ErrorResponse.class)))})
    })
    @PostMapping("/republish")
    public ResponseEntity<Object> republishOrders(@RequestBody @NotEmpty Collection<@NotBlank String> orderNumbers) {
        return executeActionOnOrderNumbers(orderNumbers, snsPublishService::publishMigrationOrderCreated);
    }

    @Operation(summary = "Start the creation process of invoices again for a given list of orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Invoices are recreated successfully"),
            @ApiResponse(responseCode  = "400", description  = "Invoice recreation of some sales orders failed", content = {
                    @Content(mediaType = "application/json", array =
                    @ArraySchema(schema = @Schema(implementation = ErrorResponse.class)))}),
            @ApiResponse(responseCode  = "400", description  = "Invalid request", content = {
                    @Content(mediaType = "application/json", array =
                    @ArraySchema(schema = @Schema(implementation = ErrorResponse.class)))})
    })
    @PostMapping("/invoice/recreate")
    public ResponseEntity<Object> recreateInvoice(@RequestBody @NotEmpty Collection<@NotBlank String> orderNumbers) {
        return executeActionOnOrderNumbers(orderNumbers, orderNumber -> {
            var salesOrder = dropshipmentOrderService.recreateSalesOrderInvoice(orderNumber);
            snsPublishService.publishSalesOrderShipmentConfirmedEvent(salesOrder, emptyList());
        });
    }

    private static ResponseEntity<Object> executeActionOnOrderNumbers(Collection<String> orderNumbers,
                                                                      Consumer<String> action) {
        var errors = new ArrayList<ErrorResponse>();
        orderNumbers.forEach(orderNumber -> {
            try {
                action.accept(orderNumber);
            } catch (Exception e) {
                errors.add(ErrorResponse.builder()
                        .orderNumber(orderNumber)
                        .errorMessage(e.getLocalizedMessage())
                        .build());
            }
        });
        return isEmpty(errors) ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body(errors);
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Map<String, String> handleValidationExceptions(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}
