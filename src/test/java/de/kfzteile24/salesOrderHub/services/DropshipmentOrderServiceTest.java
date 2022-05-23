package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentOrderServiceTest {

    @InjectMocks
    @Spy
    private DropshipmentOrderService dropshipmentOrderService;

    @Mock
    private SalesOrderService salesOrderService;

    @ParameterizedTest
    @MethodSource("provideArgumentsForIsDropShipmentOrder")
    void testIsDropShipmentOrder(String fulfillment, boolean expected) {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(fulfillment);
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));
        assertThat(dropshipmentOrderService.isDropShipmentOrder(salesOrder.getOrderNumber())).isEqualTo(expected);
    }

    private static Stream<Arguments> provideArgumentsForIsDropShipmentOrder() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(StringUtils.EMPTY, false),
                Arguments.of(K24.getName(), false),
                Arguments.of(DELTICOM.getName(), true)
        );
    }
}