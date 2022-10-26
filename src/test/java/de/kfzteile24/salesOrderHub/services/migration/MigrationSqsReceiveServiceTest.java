package de.kfzteile24.salesOrderHub.services.migration;

import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.services.MigrationCreditNoteService;
import de.kfzteile24.salesOrderHub.services.MigrationSalesOrderService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author vinaya
 */

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnusedPrivateField")
class MigrationSqsReceiveServiceTest {

    @Mock
    private FeatureFlagConfig featureFlagConfig;
    @InjectMocks
    @Spy
    private MigrationSqsReceiveService migrationSqsReceiveService;
    @Mock
    private MigrationCreditNoteService migrationCreditNoteService;
    @Mock
    private MigrationSalesOrderService migrationSalesOrderService;

    private final MessageWrapper messageWrapper = MessageWrapper.builder().build();

    @Test
    @SneakyThrows
    void testQueueListenerMigrationCoreSalesOrderCreated() {

        var message = getObjectByResource("ecpOrderMessage.json", Order.class);

        when(featureFlagConfig.getIgnoreMigrationCoreSalesOrder()).thenReturn(false);

        migrationSqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(message, messageWrapper);

        verify(migrationSalesOrderService).handleMigrationCoreSalesOrderCreated(message, messageWrapper);
    }

    @Test
    void testQueueListenerMigrationCoreSalesCreditNoteCreated() {

        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);

        when(featureFlagConfig.getIgnoreMigrationCoreSalesCreditNote()).thenReturn(false);

        migrationSqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(message, messageWrapper);

        verify(migrationCreditNoteService).handleMigrationCoreSalesCreditNoteCreated(message, messageWrapper);
    }

    @Test
    @SneakyThrows
    void testQueueListenerMigrationCoreSalesOrderCreatedNewOrder() {

        var message = getObjectByResource("ecpOrderMessage.json", Order.class);

        when(featureFlagConfig.getIgnoreMigrationCoreSalesOrder()).thenReturn(false);

        migrationSqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(message, messageWrapper);

        verify(migrationSalesOrderService).handleMigrationCoreSalesOrderCreated(message, messageWrapper);
    }
}