package de.kfzteile24.salesOrderHub.helper;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderItem;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.LogisticalUnits;
import de.kfzteile24.salesOrderHub.dto.order.Rows;
import de.kfzteile24.salesOrderHub.dto.sqs.EcpOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Component
public class SalesOrderUtil {

    @Autowired
    SalesOrderService salesOrderService;

    @Autowired
    BpmUtil bpmUtil;

    @NotNull
    @Autowired
    Gson gson;

    @NotNull
    @Autowired
    @Qualifier("messageHeader")
    Gson gsonMessage;

    public SalesOrder createNewSalesOrder() {
        InputStream testFileStream = getClass().getResourceAsStream("/examples/testmessage.json");
        assertNotNull(testFileStream);

        EcpOrder sqsMessage = readTestFile(testFileStream);
        assertNotNull(sqsMessage);

        OrderJSON orderJSON = gson.fromJson(sqsMessage.getMessage(), OrderJSON.class);
        orderJSON.getOrderHeader().setOrderNumber(bpmUtil.getRandomOrderNumber());

        final SalesOrder testOrder = de.kfzteile24.salesOrderHub.domain.SalesOrder.builder()
                .orderNumber(orderJSON.getOrderHeader().getOrderNumber())
                .salesLocale(orderJSON.getOrderHeader().getOrigin().getLocale())
                .originalOrder(orderJSON)
                .build();

        // Get Shipping Type
        List<LogisticalUnits> logisticalUnits = orderJSON.getLogisticalUnits();
        assertEquals(1, logisticalUnits.size());

        Set<SalesOrderItem> testOrderItems = new HashSet<>();

        List<Rows> orderItems = orderJSON.getOrderRows();
        orderItems.forEach(row-> {
            SalesOrderItem salesOrderItem = new SalesOrderItem();
            salesOrderItem.setQuantity( new BigDecimal(row.getQuantity().toString()));
            salesOrderItem.setStockKeepingUnit(row.getSku());
            salesOrderItem.setShippingType(logisticalUnits.get(0).getShippingType());
            testOrderItems.add(salesOrderItem);
        });

        testOrder.setSalesOrderItemList(testOrderItems);
        testOrder.setSalesOrderInvoiceList(new HashSet<>());
        salesOrderService.save(testOrder);
        return testOrder;
    }

    private EcpOrder readTestFile(InputStream testFileStream) {
        StringBuilder content = new StringBuilder();
        try (InputStreamReader streamReader =
                     new InputStreamReader(testFileStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            return gsonMessage.fromJson(content.toString(), EcpOrder.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
