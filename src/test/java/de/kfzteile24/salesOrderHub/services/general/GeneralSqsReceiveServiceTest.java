package de.kfzteile24.salesOrderHub.services.general;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderReturnService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
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
class GeneralSqsReceiveServiceTest {

    @InjectMocks
    @Spy
    private GeneralSqsReceiveService generalSqsReceiveService;
    @Mock
    private ParcelShippedService parcelShippedService;
    @Mock
    private CamundaHelper camundaHelper;
    @Mock
    private DropshipmentOrderReturnService dropshipmentOrderReturnService;

    @Test
    void testQueueListenerParcelShipped() {

        var message = getObjectByResource("parcelShipped.json", ParcelShippedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        generalSqsReceiveService.queueListenerParcelShipped(message, messageWrapper);

        verify(parcelShippedService).handleParcelShipped(message, messageWrapper);
    }

    @Test
    void testQueueListenerInvoiceReceivedFromCoreCreditNote() {

        String message = "s3://production-k24-invoices/www-kfzteile24-de/2022/10/07/987654321-2022200001.pdf";
        var messageWrapper = MessageWrapper.builder().build();

        generalSqsReceiveService.queueListenerInvoiceReceivedFromCore(message, messageWrapper);

        verify(dropshipmentOrderReturnService).handleCreditNoteFromDropshipmentOrderReturn(message, messageWrapper);
    }

    @Test
    void testQueueListenerInvoiceReceivedFromCoreInvoice() {

        String message = "s3://production-k24-invoices/www-kfzteile24-de/2022/10/07/528023111-1-2022-1000000002300.pdf";
        var messageWrapper = MessageWrapper.builder().build();

        generalSqsReceiveService.queueListenerInvoiceReceivedFromCore(message, messageWrapper);

        verify(camundaHelper).handleInvoiceFromCore(message, messageWrapper);
    }
}
