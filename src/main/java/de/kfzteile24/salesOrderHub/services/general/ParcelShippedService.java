package de.kfzteile24.salesOrderHub.services.general;

import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelShippedService {

    private final SalesOrderRowService salesOrderRowService;

    @EnrichMessageForDlq
    public void handleParcelShipped(ParcelShippedMessage message, MessageWrapper messageWrapper) {
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
}
