package de.kfzteile24.salesOrderHub.services.general;

import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
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
class GeneralSqsReceiveServiceTest {

    @InjectMocks
    @Spy
    private GeneralSqsReceiveService generalSqsReceiveService;
    @Mock
    private ParcelShippedService parcelShippedService;

    @Test
    void testQueueListenerParcelShipped() {

        var message = getObjectByResource("parcelShipped.json", ParcelShippedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        generalSqsReceiveService.queueListenerParcelShipped(message, messageWrapper);

        verify(parcelShippedService).handleParcelShipped(message, messageWrapper);
    }
}
