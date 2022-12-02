package de.kfzteile24.salesOrderHub.delegates.invoice;


import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.invoicing.AggregateInvoiceDataDelegate;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class AggregateInvoiceDataDelegateTest {
    @InjectMocks
    private AggregateInvoiceDataDelegate aggregateInvoiceDataDelegate;
    @Mock
    private DelegateExecution delegateExecution;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @Captor
    ArgumentCaptor<String> orderNumberArgCaptor;
    @Captor
    ArgumentCaptor<String> invoiceNumberArgCaptor;

    @Test
    @SneakyThrows
    void testAggregateInvoiceData() {
        //Prepare inputs
        String orderNumber1 = "orderNumber1";
        String orderNumber2 = "orderNumber2";
        String invoiceNumber1 = "2022-1000000000001";
        String invoiceNumber2 = "2022-1000000000002";
        List<DropshipmentInvoiceRow> invoiceRowList = List.of(
                DropshipmentInvoiceRow.builder().orderNumber(orderNumber1).sku("sku1.1").build(),
                DropshipmentInvoiceRow.builder().orderNumber(orderNumber1).sku("sku1.2").build(),
                DropshipmentInvoiceRow.builder().orderNumber(orderNumber2).sku("sku2.1").build()
        );
        Map<String, List<String>> invoiceDataMap = new TreeMap<>();
        invoiceDataMap.put(orderNumber1, List.of("sku1.1", "sku1.2"));
        invoiceDataMap.put(orderNumber2, List.of("sku2.1"));

        //Stub interactions
        when(dropshipmentInvoiceRowService.findAllOrderByOrderNumberAsc()).thenReturn(invoiceRowList);
        when(dropshipmentInvoiceRowService.buildInvoiceDataMap(invoiceRowList)).thenReturn(invoiceDataMap);
        when(invoiceService.createInvoiceNumber())
                .thenReturn(invoiceNumber1)
                .thenReturn(invoiceNumber2);
        doNothing().when(dropshipmentInvoiceRowService).saveInvoiceNumber(any(), any());

        //call method
        aggregateInvoiceDataDelegate.execute(delegateExecution);

        //verify that the methods are called with correct params
        verify(dropshipmentInvoiceRowService, times(2)).saveInvoiceNumber(
                orderNumberArgCaptor.capture(),
                invoiceNumberArgCaptor.capture());
        assertThat(orderNumberArgCaptor.getAllValues().get(0)).isEqualTo(orderNumber1);
        assertThat(invoiceNumberArgCaptor.getAllValues().get(0)).isEqualTo(invoiceNumber1);
        assertThat(orderNumberArgCaptor.getAllValues().get(1)).isEqualTo(orderNumber2);
        assertThat(invoiceNumberArgCaptor.getAllValues().get(1)).isEqualTo(invoiceNumber2);

        verify(delegateExecution).setVariable(
                Variables.INVOICE_NUMBER_LIST.getName(),
                List.of(invoiceNumber1, invoiceNumber2));
    }
    @Test
    @SneakyThrows
    void testAggregateInvoiceDataWithEmptyList() {

        when(dropshipmentInvoiceRowService.findAllOrderByOrderNumberAsc()).thenReturn(new ArrayList<>());
        when(dropshipmentInvoiceRowService.buildInvoiceDataMap(new ArrayList<>())).thenReturn(new TreeMap<>());

        aggregateInvoiceDataDelegate.execute(delegateExecution);

        verify(invoiceService, never()).createInvoiceNumber();
        verify(dropshipmentInvoiceRowService, never()).saveInvoiceNumber(any(), any());
        verify(delegateExecution).setVariable(Variables.INVOICE_NUMBER_LIST.getName(), new ArrayList<>());

    }
}