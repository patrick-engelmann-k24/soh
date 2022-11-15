package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.CustomValidator;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapperImpl;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.MIGRATION_SALES_ORDER_RECEIVED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.copyOrderJson;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationSalesOrderServiceTest {

    @Mock
    private OrderUtil orderUtil;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private FeatureFlagConfig featureFlagConfig;

    @Mock
    private CustomValidator customValidator;

    @Spy
    private final SalesOrderMapper salesOrderMapper = new SalesOrderMapperImpl();

    @InjectMocks
    private MigrationSalesOrderService migrationSalesOrderService;

    @SneakyThrows
    @Test
    void testHandleMigrationCoreSalesOrderCreatedDuplication() {
        var message = getObjectByResource("ecpOrderMessage.json", Order.class);
        var messageWrapper = MessageWrapper.builder().build();
        SalesOrder salesOrder = getSalesOrder(message);
        when(orderUtil.copyOrderJson(any())).thenReturn(copyOrderJson(message));
        when(salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber())).thenReturn(Optional.of(salesOrder));

        migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(message, messageWrapper);

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

        var message = getObjectByResource("ecpOrderMessage.json", Order.class);
        var messageWrapper = MessageWrapper.builder().build();
        when(orderUtil.copyOrderJson(any())).thenReturn(copyOrderJson(message));
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.empty());

        migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(message, messageWrapper);

        verify(salesOrderService).createSalesOrder(argThat(salesOrder -> {
                    assertThat(salesOrder.getOrderNumber()).isEqualTo(message.getOrderHeader().getOrderNumber());
                    assertThat(salesOrder.getLatestJson()).isEqualTo(message);
                    assertThat(salesOrder.getVersion()).isEqualTo(3L);
                    assertThat(salesOrder.getOriginalOrder()).isEqualTo(message);
                    assertThat(salesOrder.getCustomerEmail()).isEqualTo(message.getOrderHeader().getCustomer().getCustomerEmail());
                    assertThat(salesOrder.getOrderGroupId()).isEqualTo(message.getOrderHeader().getOrderGroupId());
                    assertThat(salesOrder.getSalesChannel()).isEqualTo(message.getOrderHeader().getSalesChannel());
                    return true;
                }
        ));
        verify(snsPublishService).publishMigrationOrderCreated(message.getOrderHeader().getOrderNumber());
    }
}