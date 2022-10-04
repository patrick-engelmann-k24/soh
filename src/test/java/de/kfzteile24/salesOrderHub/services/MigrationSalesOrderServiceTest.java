package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.FileUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapperImpl;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import de.kfzteile24.soh.order.dto.Order;
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
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.MIGRATION_SALES_ORDER_RECEIVED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationSalesOrderServiceTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private FeatureFlagConfig featureFlagConfig;

    @Spy
    private final SalesOrderMapper salesOrderMapper = new SalesOrderMapperImpl();

    @Mock
    private MessageWrapperUtil messageWrapperUtil;

    @InjectMocks
    private MigrationSalesOrderService migrationSalesOrderService;

    @SneakyThrows
    @Test
    void testHandleMigrationCoreSalesOrderCreatedDuplication() {
        String rawMessage = readResource("examples/ecpOrderMessage.json");
        mockMessageWrapper(rawMessage,  Order.class);
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        when(salesOrderService.getOrderByOrderNumber(eq(salesOrder.getOrderNumber()))).thenReturn(Optional.of(salesOrder));

        migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).enrichSalesOrder(salesOrder, salesOrder.getLatestJson(), (Order) salesOrder.getOriginalOrder());
        verify(salesOrderService).save(argThat(so -> {
                    assertThat(so).isEqualTo(salesOrder);
                    return true;
                }
        ), eq(MIGRATION_SALES_ORDER_RECEIVED));
        verify(snsPublishService).publishMigrationOrderCreated(salesOrder.getOrderNumber());
    }

    @Test
    @SneakyThrows
    void testHandleMigrationCoreSalesOrderCreatedNewOrder() {

        String rawMessage = readResource("examples/ecpOrderMessage.json");
        mockMessageWrapper(rawMessage,  Order.class);
        SqsMessage sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        var order = objectMapper.readValue(sqsMessage.getBody(), Order.class);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.empty());

        migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).createSalesOrder(argThat(salesOrder -> {
                    assertThat(salesOrder.getOrderNumber()).isEqualTo(order.getOrderHeader().getOrderNumber());
                    assertThat(salesOrder.getLatestJson()).isEqualTo(order);
                    assertThat(salesOrder.getVersion()).isEqualTo(3L);
                    assertThat(salesOrder.getOriginalOrder()).isEqualTo(order);
                    assertThat(salesOrder.getCustomerEmail()).isEqualTo(order.getOrderHeader().getCustomer().getCustomerEmail());
                    assertThat(salesOrder.getOrderGroupId()).isEqualTo(order.getOrderHeader().getOrderGroupId());
                    assertThat(salesOrder.getSalesChannel()).isEqualTo(order.getOrderHeader().getSalesChannel());
                    return true;
                }
        ));
        verify(snsPublishService).publishMigrationOrderCreated(order.getOrderHeader().getOrderNumber());
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