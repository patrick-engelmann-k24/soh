package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
import de.kfzteile24.salesOrderHub.helper.FileUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.mockito.Mockito.verify;

/**
 * @author vinaya
 */

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnusedPrivateField")
class FinancialDocumentsSqsReceiveServiceTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();
    @InjectMocks
    @Spy
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;
    @Mock
    private CoreSalesCreditNoteCreatedService coreSalesCreditNoteCreatedService;
    @Mock
    private SQSNamesConfig sqsNamesConfig;

    @Test
    void testQueueListenerCoreSalesCreditNoteCreated() {

        String invoiceMsg = readResource("examples/coreSalesCreditNoteCreated.json");
        String sqsName = sqsNamesConfig.getCoreSalesCreditNoteCreated();

        financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(invoiceMsg, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(coreSalesCreditNoteCreatedService).handleCoreSalesCreditNoteCreated(invoiceMsg, ANY_RECEIVE_COUNT, sqsName);

    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return FileUtil.readResource(getClass(), path);
    }
}
