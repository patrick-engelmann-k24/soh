package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class DropshipmentInvoiceRowServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @Autowired
    private DropshipmentInvoiceRowRepository dropshipmentInvoiceRowRepository;
    @Autowired
    private DropshipmentHelper dropshipmentHelper;

    @BeforeEach
    public void setup() {
        super.setUp();
        dropshipmentInvoiceRowRepository.deleteAllInBatch();
    }

    @Test
    void testCreateDropshipmentInvoiceRow() {
        DropshipmentInvoiceRow dropshipmentInvoiceRow = dropshipmentHelper.createDropshipmentInvoiceRow("sku", "orderNumber");
        dropshipmentInvoiceRowService.save(dropshipmentInvoiceRow);
        var test = dropshipmentInvoiceRowService.getBySkuAndOrderNumber("sku", "orderNumber");
        assertThat(test.get().getSku()).isEqualTo("sku");
        assertThat(test.get().getOrderNumber()).isEqualTo("orderNumber");
        test = dropshipmentInvoiceRowService.getByOrderNumber("orderNumber").stream().findFirst();
        assertThat(test.get().getSku()).isEqualTo("sku");
        assertThat(test.get().getOrderNumber()).isEqualTo("orderNumber");
    }

}