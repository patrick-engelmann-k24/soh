package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.mockito.Mockito.verify;

/**
 * @author vinaya
 */

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnusedPrivateField")
class FinancialDocumentsSqsReceiveServiceTest {

    @InjectMocks
    @Spy
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;
    @Mock
    private CoreSalesCreditNoteCreatedService coreSalesCreditNoteCreatedService;

    @Test
    void testQueueListenerCoreSalesCreditNoteCreated() {

        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(message, messageWrapper);

        verify(coreSalesCreditNoteCreatedService).handleCoreSalesCreditNoteCreated(message, messageWrapper);
    }
}
