package de.kfzteile24.salesOrderHub.services.general;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig.OBJECT_MAPPER_WITH_BEAN_VALIDATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelShippedService {

    private ObjectMapper objectMapper;
    private final SalesOrderRowService salesOrderRowService;
    private final MessageWrapperUtil messageWrapperUtil;

    @EnrichMessageForDlq
    public void handleParcelShipped(String rawMessage, Integer receiveCount, String queueName) {
        var messageWrapper =
                messageWrapperUtil.create(rawMessage, ParcelShippedMessage.class);
        var message = messageWrapper.getMessage();
        var event = message.getMessage();
        var orderNumber = event.getOrderNumber();
        log.info("Parcel shipped received with order number {}, delivery note number {}, " +
                        "tracking link: {} and order items: {}",
                orderNumber,
                event.getDeliveryNoteNumber(),
                event.getTrackingLink(),
                event.getArticleItemsDtos().stream()
                        .map(ArticleItemsDto::getNumber)
                        .collect(Collectors.toList()));

        salesOrderRowService.handleParcelShippedEvent(event);
    }
    @Autowired
    public void setObjectMapper(@Qualifier(OBJECT_MAPPER_WITH_BEAN_VALIDATION) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
