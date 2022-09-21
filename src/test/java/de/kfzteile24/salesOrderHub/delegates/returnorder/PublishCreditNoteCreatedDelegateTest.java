package de.kfzteile24.salesOrderHub.delegates.returnorder;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteCreatedEvent;
import de.kfzteile24.salesOrderHub.services.CreditNoteService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.constants.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishCreditNoteCreatedDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SalesOrderReturnService salesOrderReturnService;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private CreditNoteService creditNoteService;

    @InjectMocks
    private PublishCreditNoteCreatedDelegate publishCreditNoteCreatedDelegate;

    @Test
    @DisplayName("When Execute Publish Credit Note Created Delegate Then Expect Publish Credit Note Created Event")
    void whenExecutePublishCreditNoteCreatedDelegateThenExpectPublishCreditNoteCreatedEvent() throws Exception {
        final var expectedOrderNumber = "123";
        final var expectedCreditNoteDocumentLink = "https://test.com";

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "1234567");
        salesOrderReturn.setOrderNumber(expectedOrderNumber);

        var salesCreditNoteCreatedEvent = SalesCreditNoteCreatedEvent
                .builder()
                .returnOrder(salesOrderReturn.getReturnOrderJson())
                .creditNoteDocumentLink(expectedCreditNoteDocumentLink)
                .build();

        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(delegateExecution.getVariable(INVOICE_URL.getName())).thenReturn(expectedCreditNoteDocumentLink);
        when(salesOrderReturnService.getByOrderNumber(
                eq(expectedOrderNumber))).thenReturn(salesOrderReturn);
        when(creditNoteService.buildSalesCreditNoteCreatedEvent(eq( salesOrderReturn.getOrderNumber()), eq( expectedCreditNoteDocumentLink))).thenReturn(salesCreditNoteCreatedEvent);

        salesCreditNoteCreatedEvent = publishCreditNoteCreatedDelegate.buildSalesCreditNoteCreatedEvent(delegateExecution);
        assertThat(salesCreditNoteCreatedEvent.getCreditNoteDocumentLink()).isEqualTo(expectedCreditNoteDocumentLink);
        assertThat(salesCreditNoteCreatedEvent.getReturnOrder()).isEqualTo(salesOrderReturn.getReturnOrderJson());

        publishCreditNoteCreatedDelegate.execute(delegateExecution);

        verify(snsPublishService).publishCreditNoteCreatedEvent(salesCreditNoteCreatedEvent);

    }

}