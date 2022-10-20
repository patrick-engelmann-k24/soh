package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.exception.CorrelateOrderException;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
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

import java.util.UUID;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.PAYPAL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
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
class SalesOrderPaymentSecuredServiceTest {

    private static final String ANY_PROCESS_INSTANCE_ID = UUID.randomUUID().toString();
    private static final String ANY_PROCESS_ID = UUID.randomUUID().toString();

    @Mock
    private CamundaHelper camundaHelper;
    @Mock
    private DropshipmentOrderService dropshipmentOrderService;

    @InjectMocks
    private SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;

    @BeforeEach
    public void setup() {
        var executionEntity = new ExecutionEntity();
        executionEntity.setId(ANY_PROCESS_ID);
        executionEntity.setProcessInstanceId(ANY_PROCESS_INSTANCE_ID);
        var messageCorrelationResult =
                new MessageCorrelationResultImpl(CorrelationHandlerResult.matchedExecution(executionEntity));
        when(camundaHelper.correlateMessageByBusinessKey(any(), anyString())).thenReturn(messageCorrelationResult);
    }

    @Test
    void testCorrelateOrderReceivedPaymentSecured() {
        salesOrderPaymentSecuredService.correlateOrderReceivedPaymentSecured("4567787", "4567858");

        verify(camundaHelper, times(1)).correlateMessageByBusinessKey(
            argThat(message -> message == Messages.ORDER_RECEIVED_PAYMENT_SECURED),
            argThat(businessKey -> StringUtils.equals(businessKey, "4567787"))
        );

        verify(camundaHelper, times(1)).correlateMessageByBusinessKey(
            argThat(message -> message == Messages.ORDER_RECEIVED_PAYMENT_SECURED),
            argThat(businessKey -> StringUtils.equals(businessKey, "4567858"))
        );
    }

    @Test
    void testCorrelateOrderReceivedPaymentSecuredThrownException() {
        assertThatThrownBy(() -> salesOrderPaymentSecuredService.correlateOrderReceivedPaymentSecured("4567787", "4567858"))
                .isExactlyInstanceOf(CorrelateOrderException.class);

        verify(camundaHelper, times(1)).correlateMessageByBusinessKey(
            argThat(message -> message == Messages.ORDER_RECEIVED_PAYMENT_SECURED),
            argThat(businessKey -> StringUtils.equals(businessKey, "4567787"))
        );

        verify(camundaHelper, times(1)).correlateMessageByBusinessKey(
            argThat(message -> message == Messages.ORDER_RECEIVED_PAYMENT_SECURED),
            argThat(businessKey -> StringUtils.equals(businessKey, "4567858"))
        );
    }

    @Test
    void testHasOrderPaypalPaymentType() {
        SalesOrder salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, PAYPAL, NEW);
        Order order = salesOrder.getLatestJson();

        when(camundaHelper.getPaymentType(any())).thenReturn("paypal");

        var hasOrderPaypalPaymentType = salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(salesOrder);

        assertThat(hasOrderPaypalPaymentType).isTrue();

        verify(camundaHelper, times(1)).getPaymentType(order.getOrderHeader().getPayments());
    }

    @Test
    void testHasOrderPaymentTypeOtherAsPaypal() {

        SalesOrder salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        Order order = salesOrder.getLatestJson();

        when(camundaHelper.getPaymentType(any())).thenReturn("creditcard");

        var hasOrderPaypalPaymentType = salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(salesOrder);

        assertThat(hasOrderPaypalPaymentType).isFalse();

        verify(camundaHelper, times(1)).getPaymentType(order.getOrderHeader().getPayments());
    }

    @Test
    @SneakyThrows
    void testQueueListenerD365OrderPaymentSecuredReceived() {
        var message = getObjectByResource("d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json",  OrderPaymentSecuredMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        salesOrderPaymentSecuredService.handleD365OrderPaymentSecured(message, messageWrapper);

        verify(camundaHelper, times(2)).correlateMessageByBusinessKey(eq(Messages.ORDER_RECEIVED_PAYMENT_SECURED),
                argThat(orderNumber ->
                        StringUtils.equals(orderNumber, "4567787") ||
                                StringUtils.equals(orderNumber, "4567858"))
        );
    }

    @Test
    void testQueueListenerD365OrderPaymentSecuredDropshipmentOrderReceived() {
        var message = getObjectByResource("d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json",  OrderPaymentSecuredMessage.class);
        var messageWrapper = MessageWrapper.builder().build();


        when(dropshipmentOrderService.isDropShipmentOrder("4567787")).thenReturn(true);

        salesOrderPaymentSecuredService.handleD365OrderPaymentSecured(message, messageWrapper);

        verify(camundaHelper, times(1)).correlateMessageByBusinessKey(eq(Messages.ORDER_RECEIVED_PAYMENT_SECURED),
                argThat(orderNumber -> StringUtils.equals(orderNumber, "4567858"))
        );
    }

    @Test
    @SneakyThrows
    void testQueueListenerD365OrderPaymentSecuredReceivedThrownException() {
        var message = getObjectByResource("d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json",  OrderPaymentSecuredMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        doReturn(null).when(camundaHelper).correlateMessageByBusinessKey(any(), any());

        assertThatThrownBy(() -> salesOrderPaymentSecuredService.handleD365OrderPaymentSecured(message, messageWrapper))
                .isExactlyInstanceOf(CorrelateOrderException.class);

    }
}