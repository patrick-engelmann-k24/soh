package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.configuration.DropShipmentConfig;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.InvoiceService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.math.BigDecimal;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties(value = DropShipmentConfig.class)
class SplitOrderRecalculationServiceTest {

    @Mock
    private SalesOrderRepository orderRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private OrderUtil orderUtil;

    @InjectMocks
    private SalesOrderService salesOrderService;

    @Test
    public void recalculateOrder() {
        final var order = getOrder(readResource("examples/splitterSalesOrderMessageWithTwoRows.json"));

        salesOrderService.recalculateTotals(order);

        final var totals = order.getOrderHeader().getTotals();
        assertThat(totals.getGoodsTotalGross()).isEqualTo(BigDecimal.valueOf(101.62));
        assertThat(totals.getGoodsTotalNet()).isEqualTo(BigDecimal.valueOf(82.39));

        assertThat(totals.getShippingCostGross()).isEqualTo(BigDecimal.valueOf(5.95));
        assertThat(totals.getShippingCostNet()).isEqualTo(BigDecimal.valueOf(5));

        assertThat(totals.getTotalDiscountGross()).isEqualTo(BigDecimal.valueOf(12.2));
        assertThat(totals.getTotalDiscountNet()).isEqualTo(BigDecimal.valueOf(10.26));

        assertThat(order.getOrderHeader().getDiscounts().size()).isEqualTo(1);
    }
}
