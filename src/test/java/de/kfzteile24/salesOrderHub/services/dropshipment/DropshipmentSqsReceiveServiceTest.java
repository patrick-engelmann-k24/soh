package de.kfzteile24.salesOrderHub.services.dropshipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.FileUtil;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author vinaya
 */

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnusedPrivateField")
class DropshipmentSqsReceiveServiceTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    @Mock
    private MessageWrapperUtil messageWrapperUtil;
    @Mock
    private DropshipmentOrderService dropshipmentOrderService;
    @InjectMocks
    @Spy
    private DropshipmentSqsReceiveService dropshipmentSqsReceiveService;

    @Test
    @SneakyThrows
    void testQueueListenerDropshipmentPurchaseOrderBooked() {

        String rawMessage = readResource("examples/dropshipmentOrderPurchasedBooked.json");
        mockMessageWrapper(rawMessage,  DropshipmentPurchaseOrderBookedMessage.class);

        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentPurchaseOrderBookedMessage message =
                objectMapper.readValue(body, DropshipmentPurchaseOrderBookedMessage.class);
        verify(dropshipmentOrderService).handleDropShipmentOrderConfirmed(message);
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return FileUtil.readResource(getClass(), path);
    }

    @SneakyThrows
    private <T> void mockMessageWrapper(String rawMessage, Class<T> clazz) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        T message = objectMapper.readValue(body, clazz);
        var messageWrapper = MessageWrapper.<T>builder()
                .message(message)
                .rawMessage(rawMessage)
                .build();
        when(messageWrapperUtil.create(eq(rawMessage), eq(clazz)))
                .thenReturn(messageWrapper);
    }


}
