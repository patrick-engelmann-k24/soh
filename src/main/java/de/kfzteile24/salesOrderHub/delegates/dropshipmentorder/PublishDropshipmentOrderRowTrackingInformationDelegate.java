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

import java.util.List;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;

@Component
@Slf4j
@RequiredArgsConstructor
public class PublishDropshipmentOrderRowTrackingInformationDelegate implements JavaDelegate {

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var sku = (String) delegateExecution.getVariable(ORDER_ROW.getName());
        final var orderNumber = (String) delegateExecution.getVariable(ORDER_NUMBER.getName());
        final var trackingLinks = (List<String>) delegateExecution.getVariable(TRACKING_LINKS.getName());

        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        log.info("Publish Dropshipment Invoice Row Tracking Information for sku {} and order number {} is started", sku, orderNumber);
        snsPublishService.publishSalesOrderShipmentConfirmedEvent(salesOrder, getTrackingLinks(trackingLinks));
    }

    private List<TrackingLink> getTrackingLinks(List<String> trackingLinks) {
        return trackingLinks.stream().map(link -> getTrackingLink(link)).collect(Collectors.toList());
    }

    private TrackingLink getTrackingLink(String trackingLink) {
        try {
            return objectMapper.readValue(trackingLink, TrackingLink.class);
        } catch (Exception e) {
            return TrackingLink.builder()
                    .url(trackingLink)
                    .build();
        }
    }

}
