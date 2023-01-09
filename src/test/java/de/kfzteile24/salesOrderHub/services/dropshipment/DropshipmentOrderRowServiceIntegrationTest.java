package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentOrderRowRepository;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static org.assertj.core.api.Assertions.assertThat;

class DropshipmentOrderRowServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DropshipmentOrderRowService dropshipmentOrderRowService;
    @Autowired
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @Autowired
    private DropshipmentOrderRowRepository dropshipmentOrderRowRepository;
    @Autowired
    private DropshipmentHelper dropshipmentHelper;

    @BeforeEach
    public void setup() {
        super.setUp();
        dropshipmentOrderRowRepository.deleteAllInBatch();
    }

    @Test
    void testSaveDropshipmentOrderItems() {
        Order order = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        var orderNumber = salesOrder.getOrderNumber();
        assertThat(dropshipmentOrderRowRepository.countByOrderNumber(orderNumber)).isEqualTo(0);
        dropshipmentOrderRowService.saveDropshipmentOrderItems(salesOrder.getOrderNumber());
        assertThat(dropshipmentOrderRowRepository.countByOrderNumber(orderNumber)).isEqualTo(2);
        for (DropshipmentOrderRow dropshipmentOrderRow: dropshipmentOrderRowService.getByOrderNumber(orderNumber)) {
            boolean found = false;
            for (OrderRows orderRows: salesOrder.getLatestJson().getOrderRows()) {
                if (orderRows.getSku().equals(dropshipmentOrderRow.getSku())) {
                    assertThat(orderRows.getQuantity().intValue()).isEqualTo(dropshipmentOrderRow.getQuantity());
                    assertThat(orderNumber).isEqualTo(dropshipmentOrderRow.getOrderNumber());
                    assertThat(0).isEqualTo(dropshipmentOrderRow.getQuantityShipped());
                    found = true;
                    break;
                }
            }
            assertThat(found).isTrue();
        }
    }

    @Test
    void testCreateDropshipmentOrderRow() {
        DropshipmentOrderRow dropshipmentOrderRow = dropshipmentHelper.createDropshipmentOrderRow("sku", "orderNumber", 0);
        dropshipmentOrderRowService.save(dropshipmentOrderRow);
        var test = dropshipmentOrderRowService.getBySkuAndOrderNumber("sku", "orderNumber");
        assertThat(test.get().getSku()).isEqualTo("sku");
        assertThat(test.get().getOrderNumber()).isEqualTo("orderNumber");
        test = dropshipmentOrderRowService.getByOrderNumber("orderNumber").stream().findFirst();
        assertThat(test.get().getSku()).isEqualTo("sku");
        assertThat(test.get().getOrderNumber()).isEqualTo("orderNumber");
    }

    @Test
    void testDeleteAll() {

        DropshipmentOrderRow dropshipmentOrderRow1 = dropshipmentHelper.createDropshipmentOrderRow("sku1", "orderNumber1", 0);
        DropshipmentOrderRow dropshipmentOrderRow2 = dropshipmentHelper.createDropshipmentOrderRow("sku2", "orderNumber2", 0);
        dropshipmentOrderRowRepository.save(dropshipmentOrderRow1);
        dropshipmentOrderRowRepository.save(dropshipmentOrderRow2);

        dropshipmentOrderRowService.deleteAll();

        assertThat(dropshipmentOrderRowRepository.findAll()).hasSize(0);
    }

}