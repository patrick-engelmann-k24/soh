package de.kfzteile24.salesOrderHub.services.sqs;

import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.sqs.exception.InvalidOrderJsonException;
import de.kfzteile24.soh.order.dto.Order;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@Slf4j
class MessageWrapperUtilIntegrationTest {

    @Autowired
    private MessageWrapperUtil messageWrapperUtil;

    @SpyBean
    private SleuthHelper sleuthHelper;

    @Test
    void testCreateInvalidOrderJson() {

        String rawMessage = readResource("examples/invalidJsonMessage.json");

        assertThatThrownBy(() -> messageWrapperUtil.create(rawMessage, Order.class))
                .isInstanceOf(InvalidOrderJsonException.class);

        verify(sleuthHelper).updateTraceId("524001248");
    }

    @Test
    void testCreateValidOrderJson() {

        String rawMessage = readResource("examples/testmessage.json");

        var messageWrapper = messageWrapperUtil.create(rawMessage, Order.class);

        assertThat(messageWrapper.getMessage()).isInstanceOf(Order.class);

        verify(sleuthHelper).updateTraceId("524001248");
    }
}