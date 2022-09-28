package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.services.migration.MigrationSqsReceiveService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import static java.text.MessageFormat.format;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Slf4j
class MessageErrorHandlerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MigrationSqsReceiveService migrationSqsReceiveService;

    @Test
    @DisplayName("Test Error Handler Message for Migration Core Sales Order Created")
    void testErrorHandlerMessageForMigrationCoreSalesOrderCreated(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = "test";
        try {
            migrationSqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(rawMessage, "senderId", 3);
            fail("should throw exception");
        } catch (Exception e) {
            //ignore
        }
        verifyErrorMessageIsLogged(format("Message Error:\r\n Queue Name: {0}\r\nReceive Count: {1}\r\nContent: {2}",
                "senderId", 3, rawMessage));
    }

    @Test
    @DisplayName("Test Error Handler Message for Migration Core Sales Invoice Created")
    void testErrorHandlerMessageForMigrationCoreSalesInvoiceCreated(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        String rawMessage = "test";
        try {
            migrationSqsReceiveService.queueListenerMigrationCoreSalesInvoiceCreated(rawMessage, "senderId", 3);
        } catch (Exception e) {
            //ignore
        }
        verifyErrorMessageIsLogged(format("Message Error:\r\n Queue Name: {0}\r\nReceive Count: {1}\r\nContent: {2}",
                "senderId", 3, rawMessage));
    }

    private void verifyErrorMessageIsLogged(String message) {
        verify(messageErrorHandler).logErrorMessage(eq(message), any());
    }
}