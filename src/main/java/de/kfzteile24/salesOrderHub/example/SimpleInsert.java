package de.kfzteile24.salesOrderHub.example;

import camundajar.impl.com.google.gson.FieldNamingPolicy;
import camundajar.impl.com.google.gson.Gson;
import camundajar.impl.com.google.gson.GsonBuilder;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.Objects;

@Component
@AllArgsConstructor
public class SimpleInsert {

    @NotNull SalesOrderService service;
    @NotNull SalesOrderRepository repository;

    //@PostConstruct
    public void simpleInsert() {
        try {
            OrderJSON json = this.orderToObject();
            final SalesOrder order2 = SalesOrder.builder()
                    .orderNumber("123456")
                    .salesLocale(json.getOrderHeader().getOrigin().getLocale())
                    .originalOrder(orderAsJson(json))
                    .build();
            final SalesOrder order = service.createOrder(order2);

            order.setCustomerEmail("test@mail.de");
            final SalesOrder order1 = service.save(order);

            repository.delete(order1);
            System.out.println(order1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //    @PostConstruct
    public OrderJSON orderToObject() throws IOException {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat(DateFormat.LONG, DateFormat.LONG)
                .create();

        final OrderJSON orderJSON = gson.fromJson(loadJson(), OrderJSON.class);

        return orderJSON;
    }

    public String orderAsJson(OrderJSON order) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat(DateFormat.LONG, DateFormat.LONG)
                .create();

        return gson.toJson(order, OrderJSON.class);
    }


    private String loadJson() throws IOException {
        String fileName = "examples/order.json5";
        ClassLoader classLoader = getClass().getClassLoader();

        File file = new File(Objects.requireNonNull(classLoader.getResource(fileName)).getFile());

        //Read File Content
        return new String(Files.readAllBytes(file.toPath()));
    }
}
