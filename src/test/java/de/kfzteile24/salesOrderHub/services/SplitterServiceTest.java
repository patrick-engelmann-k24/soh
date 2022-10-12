package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.ItemSplitService;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.OrderSplitService;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.SplitOrderRecalculationService;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.services.splitter.decorator.OrderSplitService.ORDER_FULFILLMENT_K24;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SplitterServiceTest {

    @Spy
    private ObjectMapperConfig objectMapperConfig;

    @Mock
    private ItemSplitService itemSplitService;

    @Mock
    private OrderSplitService orderSplitService;

    @Mock
    private SplitOrderRecalculationService splitOrderRecalculationService;

    @Mock
    private FeatureFlagConfig featureFlagConfig;

    @InjectMocks
    private SplitterService splitterService;

    @Test
    void testSplitSalesOrder() {
        var order = getObjectByResource("coreOrderMessage.json", Order.class);
        order.getOrderHeader().setOrderFulfillment(null);

        splitterService.splitSalesOrder(order, order);
        assertEquals(ORDER_FULFILLMENT_K24, order.getOrderHeader().getOrderFulfillment());
    }
}
