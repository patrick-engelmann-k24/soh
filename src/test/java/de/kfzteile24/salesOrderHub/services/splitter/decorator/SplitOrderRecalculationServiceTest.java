package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.DropShipmentConfig;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SubsequentSalesOrderCreationHelper;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.Order;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.math.BigDecimal;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
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
    @Mock
    private SalesOrderReturnService salesOrderReturnService;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private SubsequentSalesOrderCreationHelper subsequentOrderHelper;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private SalesOrderService salesOrderService;

    @Test
    void recalculateOrder() {
        final var order = getObjectByResource("splitterSalesOrderMessageWithTwoRows.json", Order.class);

        salesOrderService.recalculateTotals(order,
                order.getOrderHeader().getTotals().getShippingCostNet(),
                order.getOrderHeader().getTotals().getShippingCostGross(),
                true);

        final var totals = order.getOrderHeader().getTotals();
        assertThat(totals.getGoodsTotalGross()).isEqualTo(BigDecimal.valueOf(101.62));
        assertThat(totals.getGoodsTotalNet()).isEqualTo(BigDecimal.valueOf(82.39));

        assertThat(totals.getShippingCostGross()).isEqualTo(BigDecimal.valueOf(5.95));
        assertThat(totals.getShippingCostNet()).isEqualTo(BigDecimal.valueOf(5));

        assertThat(totals.getSurcharges().getDepositGross()).isNull();
        assertThat(totals.getSurcharges().getDepositNet()).isNull();

        assertThat(totals.getSurcharges().getBulkyGoodsGross()).isNull();
        assertThat(totals.getSurcharges().getBulkyGoodsNet()).isNull();

        assertThat(totals.getSurcharges().getRiskyGoodsGross()).isNull();
        assertThat(totals.getSurcharges().getRiskyGoodsNet()).isNull();

        assertThat(totals.getSurcharges().getPaymentGross()).isNull();
        assertThat(totals.getSurcharges().getPaymentNet()).isNull();

        assertThat(totals.getTotalDiscountGross()).isEqualTo(BigDecimal.valueOf(12.2));
        assertThat(totals.getTotalDiscountNet()).isEqualTo(BigDecimal.valueOf(10.26));

        assertThat(totals.getGrandTotalGross()).isEqualTo(BigDecimal.valueOf(95.37));
        assertThat(totals.getGrandTotalNet()).isEqualTo(BigDecimal.valueOf(77.13));

        assertThat(totals.getGrandTotalTaxes()).hasSize(1);

        assertThat(totals.getPaymentTotal()).isEqualTo(BigDecimal.valueOf(95.37));

        assertThat(order.getOrderHeader().getDiscounts()).hasSize(1);

        assertThat(order.getOrderHeader().getDiscounts().get(0).getDiscountValueGross()).isEqualTo(BigDecimal.valueOf(0.4));
    }
}
