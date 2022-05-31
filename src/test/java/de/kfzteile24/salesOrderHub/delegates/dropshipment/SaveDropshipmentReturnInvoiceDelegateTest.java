package de.kfzteile24.salesOrderHub.delegates.dropshipment;

import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.SaveDropshipmentReturnInvoiceDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveDropshipmentReturnInvoiceDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SalesOrderReturnService salesOrderReturnService;

    @InjectMocks
    private SaveDropshipmentReturnInvoiceDelegate saveDropshipmentReturnInvoiceDelegate;

    @Test
    void testPublishDropshipmentTrackingInformationDelegate() throws Exception {
        final var expectedOrderNumber = "123-1";
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber(expectedOrderNumber);
        final var creditNoteNumber = "20222" + RandomStringUtils.randomNumeric(5);
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, creditNoteNumber);
        final var invoiceUrl = "s3://k24-invoices/www-k24-at/2020/08/12/" + expectedOrderNumber + "-" + creditNoteNumber + ".pdf";
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(delegateExecution.getVariable(INVOICE_URL.getName())).thenReturn(invoiceUrl);
        when(salesOrderReturnService.getByOrderNumber(any())).thenReturn(salesOrderReturn);

        saveDropshipmentReturnInvoiceDelegate.execute(delegateExecution);

        salesOrderReturn.setUrl(invoiceUrl);
        verify(salesOrderReturnService).save(salesOrderReturn);

    }
}