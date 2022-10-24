package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.sns.PaypalRefundInstructionSuccessfulEvent;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaypalRefundInstructionSuccessfulServiceTest {

    @Mock
    private SalesOrderReturnService salesOrderReturnService;
    @Mock
    private SnsPublishService snsPublishService;
    @Mock
    private OrderUtil orderUtil;

    @InjectMocks
    private PaypalRefundInstructionSuccessfulService paypalRefundInstructionSuccessfulService;

    @Test
    void testExceptionThrownWhenReturnOrderDoesNotExist() {
        var message = getObjectByResource("paypalRefundInstructionSuccessful.json", PaypalRefundInstructionSuccessfulEvent.class);
        var messageWrapper = MessageWrapper.builder().build();

        when(salesOrderReturnService.getReturnOrder("123456789", "123456789")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paypalRefundInstructionSuccessfulService.handlePaypalRefundInstructionSuccessful(
                message, messageWrapper))
                .isExactlyInstanceOf(SalesOrderReturnNotFoundException.class)
                .hasMessageContaining("Sales order return not found for the given order number 123456789 and credit note number 123456789 ");
    }

    @Test
    void testHandlePaypalRefundInstructionSuccessful() {
        var message = getObjectByResource("paypalRefundInstructionSuccessful.json", PaypalRefundInstructionSuccessfulEvent.class);
        var messageWrapper = MessageWrapper.builder().build();
        SalesOrderReturn salesOrderReturn = SalesOrderReturn.builder().build();

        when(salesOrderReturnService.getReturnOrder("123456789", "123456789")).thenReturn(Optional.of(salesOrderReturn));
        when(orderUtil.isDropshipmentOrder(salesOrderReturn.getReturnOrderJson())).thenReturn(true);

        paypalRefundInstructionSuccessfulService.handlePaypalRefundInstructionSuccessful(message, messageWrapper);

        verify(snsPublishService).publishPayoutReceiptConfirmationReceivedEvent(salesOrderReturn);
    }

    @Test
    void testHandlePaypalRefundInstructionSuccessfulNotDropshipmentOrder() {
        var message = getObjectByResource("paypalRefundInstructionSuccessful.json", PaypalRefundInstructionSuccessfulEvent.class);
        var messageWrapper = MessageWrapper.builder().build();
        SalesOrderReturn salesOrderReturn = SalesOrderReturn.builder().build();

        when(salesOrderReturnService.getReturnOrder("123456789", "123456789")).thenReturn(Optional.of(salesOrderReturn));
        when(orderUtil.isDropshipmentOrder(salesOrderReturn.getReturnOrderJson())).thenReturn(false);

        paypalRefundInstructionSuccessfulService.handlePaypalRefundInstructionSuccessful(message, messageWrapper);

        verify(snsPublishService, never()).publishPayoutReceiptConfirmationReceivedEvent(salesOrderReturn);
    }
}
