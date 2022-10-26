package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.constants.PersistentProperties;
import de.kfzteile24.salesOrderHub.domain.property.KeyValueProperty;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.MessageFormat;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentSqsReceiveServiceTest {

    @Mock
    private DropshipmentOrderService dropshipmentOrderService;

    @Mock
    private KeyValuePropertyService keyValuePropertyService;

    @Spy
    @InjectMocks
    private DropshipmentSqsReceiveService dropshipmentSqsReceiveService;

    private final MessageWrapper messageWrapper = MessageWrapper.builder().build();

    @Test
    void testQueueListenerDropshipmentPurchaseOrderBooked() {

        var message = getObjectByResource(
                "dropshipmentOrderPurchasedBooked.json", DropshipmentPurchaseOrderBookedMessage.class);

        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message, messageWrapper);

        verify(dropshipmentOrderService).handleDropShipmentOrderConfirmed(message, messageWrapper);
    }

    @Test
    @SneakyThrows
    void testQueueListenerDropshipmentShipmentConfirmed() {

        var message = getObjectByResource(
                "dropshipmentShipmentConfirmed.json", DropshipmentShipmentConfirmedMessage.class);

        dropshipmentSqsReceiveService.queueListenerDropshipmentShipmentConfirmed(message, messageWrapper);

        verify(dropshipmentOrderService).handleDropShipmentOrderTrackingInformationReceived(message, messageWrapper);
    }

    @Test
    void testQueueListenerDropshipmentPurchaseOrderReturnNotified() {

        var message = getObjectByResource(
                "dropshipmentPurchaseOrderReturnNotified.json", DropshipmentPurchaseOrderReturnNotifiedMessage.class);

        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderReturnNotified(message, messageWrapper);

        verify(dropshipmentOrderService).handleDropshipmentPurchaseOrderReturnNotified(message, messageWrapper);
    }

    @Test
    void testQueueListenerDropshipmentPurchaseOrderReturnConfirmed() {

        var message = getObjectByResource(
                "dropshipmentPurchaseOrderReturnConfirmed.json", DropshipmentPurchaseOrderReturnConfirmedMessage.class);

        when(keyValuePropertyService.getPropertyByKey(PersistentProperties.PREVENT_DROPSHIPMENT_ORDER_RETURN_CONFIRMED))
                .thenReturn(Optional.of(KeyValueProperty.builder().typedValue(false).build()));

        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderReturnConfirmed(message, messageWrapper);

        verify(dropshipmentOrderService).handleDropshipmentPurchaseOrderReturnConfirmed(message, messageWrapper);
    }

    @Test
    void testQueueListenerDropshipmentPurchaseOrderReturnConfirmedPrevented() {

        var message = getObjectByResource(
                "dropshipmentPurchaseOrderReturnConfirmed.json", DropshipmentPurchaseOrderReturnConfirmedMessage.class);

        when(keyValuePropertyService.getPropertyByKey(PersistentProperties.PREVENT_DROPSHIPMENT_ORDER_RETURN_CONFIRMED))
                .thenReturn(Optional.of(KeyValueProperty.builder().typedValue(true).build()));

        var expectedErrorMessage = MessageFormat.format(
                "Dropshipment Order Return Confirmed process is inactive. " +
                        "Message with Order number {0} is moved to DLQ", message.getSalesOrderNumber());

        assertThatThrownBy(() -> dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderReturnConfirmed(
                message, messageWrapper))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedErrorMessage);
    }
}
