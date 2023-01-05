package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentOrderRowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class DropshipmentOrderRowServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DropshipmentOrderRowService dropshipmentOrderRowService;
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
    void testCreateDropshipmentOrderRow() {
        DropshipmentOrderRow dropshipmentOrderRow = dropshipmentHelper.createDropshipmentOrderRow("sku", "orderNumber");
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

        DropshipmentOrderRow dropshipmentOrderRow1 = dropshipmentHelper.createDropshipmentOrderRow("sku1", "orderNumber1");
        DropshipmentOrderRow dropshipmentOrderRow2 = dropshipmentHelper.createDropshipmentOrderRow("sku2", "orderNumber2");
        dropshipmentOrderRowRepository.save(dropshipmentOrderRow1);
        dropshipmentOrderRowRepository.save(dropshipmentOrderRow2);

        dropshipmentOrderRowService.deleteAll();

        assertThat(dropshipmentOrderRowRepository.findAll()).hasSize(0);
    }

}