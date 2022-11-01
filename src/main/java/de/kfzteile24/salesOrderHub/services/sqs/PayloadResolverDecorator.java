package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.soh.order.dto.Order;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;

import static de.kfzteile24.salesOrderHub.services.sqs.AbstractSqsReceiveService.logIncomingMessage;

@Configuration
public class PayloadResolverDecorator extends PayloadMethodArgumentResolver {

    private final SleuthHelper sleuthHelper;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public PayloadResolverDecorator(SleuthHelper sleuthHelper,
                                    MessageConverter messageConverter,
                                    Validator validator,
                                    ObjectMapper objectMapper,
                                    Environment environment) {
        super(messageConverter, validator);
        this.sleuthHelper = sleuthHelper;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    /**
     * If the payload parameter is not the first one, should be annotated by {@link Payload}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Payload.class) || parameter.getParameterIndex() == 0;
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, @NonNull Message<?> message) throws Exception {
        String rawMessage;
        if (isDefaultProfile()) {
            var sqsMessage = objectMapper.readValue(message.getPayload().toString(), SqsMessage.class);
            rawMessage = sqsMessage.getBody();
        } else {
            rawMessage = message.getPayload().toString();
        }
        logIncomingMessage(message, rawMessage);
        var parameterClass = parameter.getParameterType();

        if (!parameterClass.isAssignableFrom(String.class)) {
            Object payload = objectMapper.readValue(rawMessage, parameterClass);
            validate(message, parameter, payload);
            return payload;
        }
        return rawMessage;
    }

    /**
     * Payload to be validated, should be annotated by {@link Validated}
     */
    @Override
    protected void validate(@NonNull Message<?> message, @NonNull MethodParameter parameter, @NonNull Object target) {
        updateTraceId(target);
        super.validate(message, parameter, target);
    }

    public boolean isDefaultProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .noneMatch(profile -> StringUtils.containsIgnoreCase(profile, "local") ||
                        StringUtils.containsIgnoreCase(profile, "model") ||
                        StringUtils.containsIgnoreCase(profile, "test"));
    }

    private <T> void updateTraceId(T json) {
        sleuthHelper.updateTraceId(resolveTraceId(json));
    }

    private static <T> String resolveTraceId(T json) {
        if (json == null) {
            return StringUtils.EMPTY;
        }
        if (json.getClass().isAssignableFrom(Order.class)) {
            return ((Order)json).getOrderHeader().getOrderNumber();
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
