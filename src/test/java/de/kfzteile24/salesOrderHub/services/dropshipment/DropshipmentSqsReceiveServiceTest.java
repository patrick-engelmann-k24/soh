package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DropshipmentSqsReceiveServiceTest {

    @Mock
    private DropshipmentOrderService dropshipmentOrderService;

    @InjectMocks
    @Spy
    private DropshipmentSqsReceiveService dropshipmentSqsReceiveService;

    @Test
    @SneakyThrows
    void testQueueListenerDropshipmentPurchaseOrderBooked() {

        var message = getObjectByResource("dropshipmentOrderPurchasedBooked.json", DropshipmentPurchaseOrderBookedMessage.class);

        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message);

        verify(dropshipmentOrderService).handleDropShipmentOrderConfirmed(message);
    }
}
