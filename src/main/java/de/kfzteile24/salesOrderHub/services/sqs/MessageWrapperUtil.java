package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.sqs.exception.InvalidOrderJsonException;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolationException;

import static de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig.OBJECT_MAPPER_WITH_BEAN_VALIDATION;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageWrapperUtil {

    private final ObjectMapper objectMapperNoValidation;
    private final SleuthHelper sleuthHelper;
    private ObjectMapper objectMapper;

    @SneakyThrows(JsonProcessingException.class)
    public <T> MessageWrapper<T> create(String rawMessage, Class<T> messageType) {
        SqsMessage sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        return MessageWrapper.<T>builder()
                .sqsMessage(sqsMessage)
                .message(getMessage(messageType, sqsMessage))
                .rawMessage(rawMessage)
                .build();
    }

    public <T> T createMessage(String rawMessage, Class<T> messageType) {
        return create(rawMessage, messageType).getMessage();
    }

    @Autowired
    public void setObjectMapper(@Qualifier(OBJECT_MAPPER_WITH_BEAN_VALIDATION) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private <T> T getMessage(Class<T> messageType, SqsMessage sqsMessage) {
        T json;
        try {
            if (messageType.equals(String.class)) {
                json = (T) sqsMessage.getBody();
            } else {
                json = objectMapper.readValue(sqsMessage.getBody(), messageType);
            }
            updateTraceId(json);
            return json;
        } catch (ConstraintViolationException e1) {
            updateTraceId(messageType, sqsMessage, e1.getMessage());
            throw new InvalidOrderJsonException(e1.getMessage());
        } catch (JsonProcessingException e2) {
            updateTraceId(messageType, sqsMessage, e2.getCause().getMessage());
            throw new InvalidOrderJsonException(e2);
        }
    }

    @SneakyThrows({ConstraintViolationException.class, JsonProcessingException.class})
    private <T> void updateTraceId(Class<T> messageType, SqsMessage sqsMessage, String errormessage) {
        T json = objectMapperNoValidation.readValue(sqsMessage.getBody(), messageType);
        updateTraceId(json);
        log.error(errormessage);
    }

    private <T> void updateTraceId(T json) {
        sleuthHelper.updateTraceId(resolveTraceId(json));
    }

    private <T> String resolveTraceId(T json) {
        if (json == null) {
            return StringUtils.EMPTY;
        }
        if (json.getClass().isAssignableFrom(Order.class)) {
            return ((Order)json).getOrderHeader().getOrderNumber();
        } else if (json.getClass().isAssignableFrom(FulfillmentMessage.class)) {
            return ((FulfillmentMessage)json).getOrderNumber();
        } else if (json.getClass().isAssignableFrom(CoreDataReaderEvent.class)) {
            return ((CoreDataReaderEvent)json).getOrderNumber();
        } else if (json.getClass().isAssignableFrom(String.class)) {
            return InvoiceUrlExtractor.extractOrderNumber((String)json);
        } else if (json.getClass().isAssignableFrom(OrderPaymentSecuredMessage.class)) {
            return ((OrderPaymentSecuredMessage)json).getData().getOrderGroupId();
        } else if (json.getClass().isAssignableFrom(DropshipmentShipmentConfirmedMessage.class)) {
            return ((DropshipmentShipmentConfirmedMessage)json).getSalesOrderNumber();
        } else if (json.getClass().isAssignableFrom(DropshipmentPurchaseOrderBookedMessage.class)) {
            return ((DropshipmentPurchaseOrderBookedMessage)json).getSalesOrderNumber();
        } else if (json.getClass().isAssignableFrom(DropshipmentPurchaseOrderReturnConfirmedMessage.class)) {
            return ((DropshipmentPurchaseOrderReturnConfirmedMessage)json).getSalesOrderNumber();
        } else if (json.getClass().isAssignableFrom(DropshipmentPurchaseOrderReturnNotifiedMessage.class)) {
            return ((DropshipmentPurchaseOrderReturnNotifiedMessage)json).getSalesOrderNumber();
        } else if (json.getClass().isAssignableFrom(SalesCreditNoteCreatedMessage.class)) {
            return ((SalesCreditNoteCreatedMessage)json).getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        } else if (json.getClass().isAssignableFrom(CoreSalesInvoiceCreatedMessage.class)) {
            return ((CoreSalesInvoiceCreatedMessage)json).getSalesInvoice().getSalesInvoiceHeader().getOrderNumber();
        } else if (json.getClass().isAssignableFrom(ParcelShippedMessage.class)) {
            return ((ParcelShippedMessage)json).getMessage().getOrderNumber();
        }

        return StringUtils.EMPTY;
    }
}
