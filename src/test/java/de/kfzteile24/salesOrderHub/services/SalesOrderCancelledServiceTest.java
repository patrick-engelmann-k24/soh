package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.exception.CorrelateOrderException;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.runtime.CorrelationHandlerResult;
import org.camunda.bpm.engine.impl.runtime.MessageCorrelationResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.PAYPAL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesOrderCancelledServiceTest {

    private static final String ANY_PROCESS_INSTANCE_ID = UUID.randomUUID().toString();
    private static final String ANY_PROCESS_ID = UUID.randomUUID().toString();

    @Mock
    private SalesOrderRowService salesOrderRowService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private SalesOrderCancelledService salesOrderCancelledService;

    @Test
    void testHandleCoreSalesOrderCancelled() {
        var message = getObjectByResource("coreSalesOrderCancelledMessage.json",  CoreSalesOrderCancelledMessage.class);
        var messageWrapper = MessageWrapper.builder().build();
        var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.getLatestJson().getOrderRows().forEach(orderRows -> orderRows.setIsCancelled(true));
        var orderNumber = salesOrder.getOrderNumber();
        message.setOrderNumber(orderNumber);
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));

        salesOrderCancelledService.handleCoreSalesOrderCancelled(message, messageWrapper);

        verify(salesOrderRowService).cancelOrder(eq(salesOrder));
        verify(salesOrderRowService).cancelOrderRows(eq(orderNumber), any());
        verify(snsPublishService).publishOrderCancelled(eq(salesOrder.getLatestJson()));
    }
}