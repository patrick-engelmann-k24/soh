package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class MessageWrapperUtilTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @InjectMocks
    @Spy
    private MessageWrapperUtil messageWrapperUtil;

    @Mock
    private SleuthHelper sleuthHelper;

    @Test
    void testCreate() throws JsonProcessingException {

        messageWrapperUtil.setObjectMapper(objectMapper);

        String rawMessage = readResource("examples/coreOrderMessage.json");
        var sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        var order = objectMapper.readValue(sqsMessage.getBody(), Order.class);

        var messageWrapper = messageWrapperUtil.create(rawMessage, Order.class);

        assertThat(messageWrapper.getRawMessage()).isEqualTo(rawMessage);
        assertThat(messageWrapper.getSqsMessage()).isEqualTo(sqsMessage);
        assertThat(messageWrapper.getMessage()).isEqualTo(order);
    }

    @Test
    void testCreateForStringMessage() throws JsonProcessingException {

        messageWrapperUtil.setObjectMapper(objectMapper);

        String rawMessage = readResource("examples/invoicesFromCoreMessage.json");
        var sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        var invoicesFromCoreMessage = sqsMessage.getBody();

        var messageWrapper = messageWrapperUtil.create(rawMessage, String.class);

        assertThat(messageWrapper.getRawMessage()).isEqualTo(rawMessage);
        assertThat(messageWrapper.getSqsMessage()).isEqualTo(sqsMessage);
        assertThat(messageWrapper.getMessage()).isEqualTo(invoicesFromCoreMessage);
    }
}