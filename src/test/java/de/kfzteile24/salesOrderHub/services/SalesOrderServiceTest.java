package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.UnusedPrivateMethod"})
class SalesOrderServiceTest {
    @Mock
    private CamundaHelper camundaHelper;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private RuntimeService runtimeService;

    @InjectMocks
    private SalesOrderService salesOrderService;

    @ParameterizedTest
    @MethodSource("provideParamsForRecurringOrderTest")
    public void recurringOrdersAreIdentifiedCorrectly(long orderCount, boolean expectedResult) {
        final var salesOrder = getSalesOrder(readResource("examples/ecpOrderMessage.json"));

        when(salesOrderRepository.countByCustomerEmail(salesOrder.getCustomerEmail())).thenReturn(orderCount);

        final boolean isRecurring = salesOrderService.isRecurringOrder(salesOrder);
        assertThat(isRecurring).isEqualTo(expectedResult);
    }

    @Test
    public void savingAnOrderAlsoInsertsAnAuditLogEntry() {
        var salesOrder = getSalesOrder(readResource("examples/ecpOrderMessage.json"));
        salesOrder.setId(UUID.randomUUID());
        when(salesOrderRepository.save(eq(salesOrder))).thenReturn(salesOrder);

        salesOrderService.save(salesOrder, ORDER_CREATED);

        verify(salesOrderRepository).save(salesOrder);
        verify(auditLogRepository).save(argThat(auditLog -> {
            assertThat(auditLog.getSalesOrderId()).isEqualTo(salesOrder.getId());
            assertThat(auditLog.getAction()).isEqualTo(ORDER_CREATED);
            assertThat(auditLog.getData()).isEqualTo(salesOrder.getLatestJson());
            return true;
        }));
    }

    private static Stream<Arguments> provideParamsForRecurringOrderTest() {
        return Stream.of(
                Arguments.of(1L, true),
                Arguments.of(0L, false)
        );
    }

}