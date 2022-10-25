package de.kfzteile24.salesOrderHub.delegates.returnorder;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteDocumentGeneratedEvent;
import de.kfzteile24.salesOrderHub.services.financialdocuments.CreditNoteService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishCreditNoteDocumentGeneratedDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SalesOrderReturnService salesOrderReturnService;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private CreditNoteService creditNoteService;

    @InjectMocks
    private PublishCreditNoteDocumentGeneratedDelegate publishCreditNoteDocumentGeneratedDelegate;

    @Test
    @DisplayName("When Execute Publish Credit Note Document Generated Delegate Then Expect Publish Credit Note Document Generated Event")
    void whenExecutePublishCreditNoteDocumentGeneratedDelegateThenExpectPublishCreditNoteDocumentGeneratedEvent() throws Exception {
        final var expectedOrderNumber = "123";
        final var expectedCreditNoteDocumentLink = "https://test.com";

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "1234567");
        salesOrderReturn.setOrderNumber(expectedOrderNumber);

        var creditNoteGeneratedEvent = SalesCreditNoteDocumentGeneratedEvent
                .builder()
                .returnOrder(salesOrderReturn.getReturnOrderJson())
                .creditNoteDocumentLink(expectedCreditNoteDocumentLink)
                .build();

        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(delegateExecution.getVariable(INVOICE_URL.getName())).thenReturn(expectedCreditNoteDocumentLink);
        when(salesOrderReturnService.getByOrderNumber(eq(expectedOrderNumber))).thenReturn(Optional.of(salesOrderReturn));
        when(creditNoteService.buildSalesCreditNoteDocumentGeneratedEvent(
                eq(salesOrderReturn.getOrderNumber()),
                eq(expectedCreditNoteDocumentLink))).thenReturn(creditNoteGeneratedEvent);

        creditNoteGeneratedEvent = publishCreditNoteDocumentGeneratedDelegate.buildSalesCreditNoteDocumentGeneratedEvent(delegateExecution);
        assertThat(creditNoteGeneratedEvent.getCreditNoteDocumentLink()).isEqualTo(expectedCreditNoteDocumentLink);
        assertThat(creditNoteGeneratedEvent.getReturnOrder()).isEqualTo(salesOrderReturn.getReturnOrderJson());

        publishCreditNoteDocumentGeneratedDelegate.execute(delegateExecution);

        verify(snsPublishService).publishCreditNoteDocumentGeneratedEvent(creditNoteGeneratedEvent);

    }

}