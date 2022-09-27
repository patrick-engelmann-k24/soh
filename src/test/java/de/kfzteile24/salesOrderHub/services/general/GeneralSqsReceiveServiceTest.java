package de.kfzteile24.salesOrderHub.services.general;

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
class GeneralSqsReceiveServiceTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();
    @InjectMocks
    @Spy
    private GeneralSqsReceiveService generalSqsReceiveService;
    @Mock
    private ParcelShippedService parcelShippedService;
    @Mock
    private SQSNamesConfig sqsNamesConfig;

    @Test
    void testQueueListenerParcelShipped() {

        String invoiceMsg = readResource("examples/parcelShipped.json");
        String sqsName = sqsNamesConfig.getParcelShipped();

        generalSqsReceiveService.queueListenerParcelShipped(invoiceMsg, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(parcelShippedService).handleParcelShipped(invoiceMsg, ANY_RECEIVE_COUNT, sqsName);

    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return FileUtil.readResource(getClass(), path);
    }
}
