package de.kfzteile24.salesOrderHub.controller;

import com.google.gson.Gson;
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
    private SalesOrderRepository orderRepository;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SalesOrderItemService salesOrderItemService;

    @Autowired
    CamundaHelper camundaHelper;

    private RuntimeService runtimeService;

    @Autowired
    private Gson gson;

    @Autowired
    @Qualifier("messageHeader")
    private Gson gsonMessageHeader;

    @PostMapping("/fake/{orderNumber}")
    @SneakyThrows
    public OrderJSON createFakeOrder(@PathVariable String orderNumber) {
        FileReader fR = new FileReader("/Users/robert/PROJECTS/k24/soh2/src/test/resources/examples/testmessage.json");

        EcpOrder eo = gsonMessageHeader.fromJson(fR, EcpOrder.class);
        OrderJSON orderJSON = gson.fromJson(eo.getMessage(), OrderJSON.class);

        orderJSON.getOrderHeader().setOrderNumber(orderNumber);

        Address billing = orderJSON.getOrderHeader().getBillingAddress();
        Address shipping = orderJSON.getOrderHeader().getShippingAddress();

        SalesOrderAddress soaBilling = SalesOrderAddress.builder()
                .addressType("billing")
                .company(billing.getCompany())
                .firstName(billing.getFirstName())
                .lastName(billing.getLastName())
                .street1(billing.getStreet1())
                .street2(billing.getStreet2())
                .street3(billing.getStreet3())
                .city(billing.getCity())
                .zipCode(billing.getZipCode())
                .taxNumber(billing.getTaxNumber())
                .build();
        SalesOrderAddress soaShipping = null;
        if (shipping != null) {
             soaShipping = SalesOrderAddress.builder()
                    .addressType("shipping")
                    .company(shipping.getCompany())
                    .firstName(shipping.getFirstName())
                    .lastName(shipping.getLastName())
                    .street1(shipping.getStreet1())
                    .street2(shipping.getStreet2())
                    .street3(shipping.getStreet3())
                    .city(shipping.getCity())
                    .zipCode(shipping.getZipCode())
                    .taxNumber(shipping.getTaxNumber())
                    .build();
        }


        final SalesOrder salesOrder = SalesOrder.builder()
                .orderNumber(orderJSON.getOrderHeader().getOrderNumber())
                .originalOrder(orderJSON)
                .salesLocale(orderJSON.getOrderHeader().getOrigin().getLocale())
                .salesOrderAddressList(new HashSet<>(Arrays.asList(soaBilling, soaShipping)))
                .build();
        orderRepository.save(salesOrder);
        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder);
        salesOrder.setProcessId(UUID.fromString(orderProcess.getRootProcessInstanceId()));
        orderRepository.save(salesOrder);

        return orderJSON;
    }

    @GetMapping("/{orderNumber}/isChangeable")
    public Boolean orderIsChangeable(@PathVariable String orderNumber) {
        return false;
    }

    @GetMapping("/{orderNumber}/isBillingAddressChangeable")
    public Boolean isBillingAddressChangeable(@PathVariable String orderNumber) {
        return !salesOrderService.isOrderBillingAddressChangeable(orderNumber);
    }

    @GetMapping("/{orderNumber}/billingAdresss")
    public Address getBillingAddress(@PathVariable String orderNumber) {
        final Optional<SalesOrder> order = salesOrderService.getOrderByOrderNumber(orderNumber);
        return order.map(salesOrder -> salesOrder.getOriginalOrder().getOrderHeader().getBillingAddress()).orElse(null);
    }

    @PutMapping("/{orderNumber}/billingAddress")
    @ResponseStatus(HttpStatus.OK)
    public Address updateBillingAddress(
            @PathVariable("orderNumber") final String orderNumber, @RequestBody final Address address) {
        final Optional<SalesOrder> order = salesOrderService.getOrderByOrderNumber(orderNumber);
        if (order.isPresent()) {
            final SalesOrder salesOrder = salesOrderService.updateOrderBillingAddress(order.get(), address);
            return salesOrder.getOriginalOrder().getOrderHeader().getBillingAddress();
        }

        return null;
    }

    @GetMapping("/{orderNumber}")
    public OrderJSON getOrder(@PathVariable String orderNumber) {
        final Optional<SalesOrder> salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        if (salesOrder.isEmpty()) {
            return null;
        }

        return salesOrder.get().getOriginalOrder();
    }

    @GetMapping("/{orderNumber}/{itemPos}/isDeliveryAddressChangeable")
    public Boolean isDeliveryAddressChangeable(@PathVariable String orderNumber, @PathVariable Integer itemPos) {
        final Optional<SalesOrder> soOpt = orderRepository.getOrderByOrderNumber(orderNumber);
        if (soOpt.isPresent()) {
            final SalesOrder so = soOpt.get();
            // todo: check if this is really working for all subprocesses.
            final Boolean aBoolean = salesOrderItemService.itemChangeable(so.getProcessId().toString(), so.getOriginalOrder().getLogisticalUnits().get(0).getShippingType());
            System.out.println(aBoolean);
        }

        return false;
    }


}
