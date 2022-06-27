package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;

@Component
@Slf4j
@RequiredArgsConstructor
public class PublishDropshipmentTrackingInformationDelegate implements JavaDelegate {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(ORDER_NUMBER.getName());
        final var trackingList = (List<String>) delegateExecution.getVariable(TRACKING_LINKS.getName());
        final Collection<TrackingLink> trackingLinks = getTrackingLinks(trackingList);
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        log.info("Publish Dropshipment Tracking Information process with order number {} is started", orderNumber);
        snsPublishService.publishSalesOrderShipmentConfirmedEvent(salesOrder, trackingLinks);
    }

    private Collection<TrackingLink> getTrackingLinks(List<String> trackingList) {
        try {
            Collection<TrackingLink> trackingLinks = new ArrayList<>();
            for (String item : trackingList) {
                trackingLinks.add(objectMapper.readValue(item, TrackingLink.class));
            }
            return trackingLinks;
        } catch (Exception e) {
            return trackingList.stream().map(url -> TrackingLink.builder()
                    .url(url)
                    .build()
            ).collect(Collectors.toList());
        }
    }

}
