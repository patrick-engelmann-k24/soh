package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderHeader;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

@Service
@RequiredArgsConstructor
@Value
@Slf4j
@NonFinal
public class SalesOrderPaymentSecuredService {

    @NonNull
    CamundaHelper camundaHelper;

    private static String collectErrorMessages(ArrayList<Exception> exceptions) {
        return exceptions.stream()
                .map(Exception::getMessage)
                .collect(joining("---------------\r\n\r\n"));
    }

    /**
     * Correlate {@link Messages#ORDER_RECEIVED_PAYMENT_SECURED message} of one or more process instances by
     * business key ensuring all process instance tried to be correlated even though some of correlations thrown exception.
     * <p>
     * An exception with collected error message will be thrown if at least one of correlations failed.
     *
     * @param orderNumbers One or multiple order numbers (business key)
     */
    public void correlateOrderReceivedPaymentSecured(String... orderNumbers) {
        var exceptions = new ArrayList<Exception>();

        for (String orderNumber : orderNumbers) {
            correlateOrderReceivedPaymentSecured(orderNumber, exceptions);
        }

        if (CollectionUtils.isNotEmpty(exceptions)) {
            throw new RuntimeException(collectErrorMessages(exceptions));
        }
    }

    public boolean hasOrderPaypalPaymentType(SalesOrder salesOrder) {
        return Optional.of(salesOrder)
                .map(SalesOrder::getLatestJson)
                .map(Order::getOrderHeader)
                .map(OrderHeader::getPayments)
                .map(camundaHelper::getPaymentType)
                .map(paymentType -> StringUtils.equalsIgnoreCase(paymentType, PaymentType.PAYPAL.getName()))
                .orElse(Boolean.FALSE);
    }

    private void correlateOrderReceivedPaymentSecured(String orderNumber, Collection<Exception> exceptions) {
        try {
            var result =
                    camundaHelper.correlateMessageByBusinessKey(Messages.ORDER_RECEIVED_PAYMENT_SECURED,
                            orderNumber);

            if (!result.getExecution().getProcessInstanceId().isEmpty()) {
                log.info("Order payment secured message for order number {} successfully received", orderNumber);
            }
        } catch (Exception e) {
            log.error("Order payment secured message error:\r\nOrderNumber: {}\r\nError-Message: {}", orderNumber,
                    e.getMessage()
            );
            exceptions.add(e);
        }
    }
}
