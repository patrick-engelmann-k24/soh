package de.kfzteile24.salesOrderHub.services.dropshipment;


import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DropshipmentInvoiceRowServiceTest {

    @Mock
    private DropshipmentInvoiceRowRepository dropshipmentInvoiceRowRepository;

    @Mock
    private DropshipmentHelper dropshipmentHelper;
    @InjectMocks
    @Spy
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Test
    void testMergeRowsByOrderNumberAndSku() {
        //Prepare inputs
        String orderNumber1 = "orderNumber1";
        String orderNumber2 = "orderNumber2";
        List<DropshipmentInvoiceRow> invoiceRowList = List.of(
                DropshipmentInvoiceRow.builder().orderNumber(orderNumber1).sku("sku1.1").quantity(2).build(),
                DropshipmentInvoiceRow.builder().orderNumber(orderNumber1).sku("sku1.1").quantity(3).build(),
                DropshipmentInvoiceRow.builder().orderNumber(orderNumber2).sku("sku2.1").quantity(2).build(),
                DropshipmentInvoiceRow.builder().orderNumber(orderNumber2).sku("sku2.2").quantity(3).build()
        );
        Set<String> orderNumberSet = new TreeSet<>();
        orderNumberSet.add(orderNumber1);
        orderNumberSet.add(orderNumber2);

        List<DropshipmentInvoiceRow> result = dropshipmentInvoiceRowService.mergeRowsByOrderNumberAndSku(invoiceRowList);
        verify(dropshipmentInvoiceRowService).delete(eq(invoiceRowList.get(1)));
        verify(dropshipmentInvoiceRowService, never()).delete(eq(invoiceRowList.get(0)));
        verify(dropshipmentInvoiceRowService, never()).delete(eq(invoiceRowList.get(2)));
        verify(dropshipmentInvoiceRowService, never()).delete(eq(invoiceRowList.get(3)));

        verify(dropshipmentInvoiceRowService).save(eq(invoiceRowList.get(0)));
        verify(dropshipmentInvoiceRowService, never()).save(eq(invoiceRowList.get(1)));
        verify(dropshipmentInvoiceRowService, never()).save(eq(invoiceRowList.get(2)));
        verify(dropshipmentInvoiceRowService, never()).save(eq(invoiceRowList.get(3)));

        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0).getOrderNumber()).isEqualTo(orderNumber1);
        assertThat(result.get(0).getSku()).isEqualTo("sku1.1");
        assertThat(result.get(0).getQuantity()).isEqualTo(5);
        assertThat(result.get(1).getOrderNumber()).isEqualTo(orderNumber2);
        assertThat(result.get(1).getSku()).isEqualTo("sku2.1");
        assertThat(result.get(1).getQuantity()).isEqualTo(2);
        assertThat(result.get(2).getOrderNumber()).isEqualTo(orderNumber2);
        assertThat(result.get(2).getSku()).isEqualTo("sku2.2");
        assertThat(result.get(2).getQuantity()).isEqualTo(3);
    }
}