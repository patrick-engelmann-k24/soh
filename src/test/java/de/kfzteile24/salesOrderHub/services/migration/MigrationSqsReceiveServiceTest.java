package de.kfzteile24.salesOrderHub.services.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.helper.FileUtil;
import de.kfzteile24.salesOrderHub.services.MigrationCreditNoteService;
import de.kfzteile24.salesOrderHub.services.MigrationSalesOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
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
import static org.mockito.Mockito.when;

/**
 * @author vinaya
 */

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnusedPrivateField")
class MigrationSqsReceiveServiceTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    private ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    @Mock
    private MessageWrapperUtil messageWrapperUtil;
    @Mock
    private FeatureFlagConfig featureFlagConfig;
    @InjectMocks
    @Spy
    private MigrationSqsReceiveService migrationSqsReceiveService;
    @Mock
    private MigrationCreditNoteService migrationCreditNoteService;
    @Mock
    private MigrationSalesOrderService migrationSalesOrderService;

    @Test
    @SneakyThrows
    void testQueueListenerMigrationCoreSalesOrderCreated() {

        String rawMessage = readResource("examples/ecpOrderMessage.json");

        when(featureFlagConfig.getIgnoreMigrationCoreSalesOrder()).thenReturn(false);

        migrationSqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(migrationSalesOrderService).handleMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);
    }

    @Test
    void testQueueListenerMigrationCoreSalesCreditNoteCreated() {

        String rawEventMessage = readResource("examples/coreSalesCreditNoteCreated.json");

        when(featureFlagConfig.getIgnoreMigrationCoreSalesCreditNote()).thenReturn(false);

        migrationSqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(rawEventMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(migrationCreditNoteService).handleMigrationCoreSalesCreditNoteCreated(rawEventMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);
    }

    @Test
    @SneakyThrows
    void testQueueListenerMigrationCoreSalesOrderCreatedNewOrder() {

        String rawMessage = readResource("examples/ecpOrderMessage.json");

        when(featureFlagConfig.getIgnoreMigrationCoreSalesOrder()).thenReturn(false);

        migrationSqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(migrationSalesOrderService).handleMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return FileUtil.readResource(getClass(), path);
    }
}