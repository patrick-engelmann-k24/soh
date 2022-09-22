package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.services.CreditNoteService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreditNoteServiceTest {

    @Mock
    private SalesOrderReturnService salesOrderReturnService;

    @InjectMocks
    private CreditNoteService creditNoteService;

    @Test
    @DisplayName("When Build Sales Credit Note Created Event Then Return Expected Result")
    void whenBuildSalesCreditNoteCreatedEventThenReturnExpectedResult() throws Exception {
        final var expectedOrderNumber = "123";
        final var expectedCreditNoteDocumentLink = "https://test.com";

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "1234567");
        salesOrderReturn.setOrderNumber(expectedOrderNumber);

        when(salesOrderReturnService.getByOrderNumber(eq(expectedOrderNumber))).thenReturn(salesOrderReturn);

        var salesCreditNoteCreatedEvent = creditNoteService.buildSalesCreditNoteCreatedEvent(expectedOrderNumber, expectedCreditNoteDocumentLink);
        assertThat(salesCreditNoteCreatedEvent.getCreditNoteDocumentLink()).isEqualTo(expectedCreditNoteDocumentLink);
        assertThat(salesCreditNoteCreatedEvent.getReturnOrder()).isEqualTo(salesOrderReturn.getReturnOrderJson());
    }
}
