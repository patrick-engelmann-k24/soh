package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.sns.PaypalRefundInstructionSuccessfulEvent;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaypalRefundInstructionSuccessfulService {

    private final SalesOrderReturnService salesOrderReturnService;
    private final SnsPublishService snsPublishService;
    private final OrderUtil orderUtil;

    @SneakyThrows
    @EnrichMessageForDlq
    public void handlePaypalRefundInstructionSuccessful(PaypalRefundInstructionSuccessfulEvent message,
                                                        MessageWrapper messageWrapper) {

        var orderNumber = message.getEvent().getOrderNumber();
        var creditNoteNumber = message.getEvent().getPaypalRequestPayload().getCreditNoteNumber();

        log.info("Received core sales credit note created message with order number: {} and credit note number:{}",
                orderNumber,
                creditNoteNumber);
        SalesOrderReturn salesOrderReturn = salesOrderReturnService.getReturnOrder(orderNumber, creditNoteNumber)
                .orElseThrow(() -> new SalesOrderReturnNotFoundException(orderNumber, creditNoteNumber));

        if (orderUtil.isDropshipmentOrder(salesOrderReturn.getReturnOrderJson())) {
            snsPublishService.publishPayoutReceiptConfirmationReceivedEvent(salesOrderReturn);
        } else {
            log.info("Return order searched with order number {} and credit note number {} is not dropshipment, " +
                            "paypal refund message is ignored",
                    orderNumber,
                    creditNoteNumber);
        }
    }
}
