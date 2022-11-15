package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShipped;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

class SalesOrderRowServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SalesOrderRowService salesOrderRowService;

    @Autowired
    private AuditLogUtil auditLogUtil;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private CamundaHelper camundaHelper;

    @Test
    void testHandleParcelShippedEvent() {
        final SalesOrder salesOrder1 = salesOrderUtil.createPersistedSalesOrder(LocalDateTime.now(), "sku1");
        final SalesOrder salesOrder2 = salesOrderUtil.createPersistedSalesOrder(
                salesOrder1.getOrderNumber(), LocalDateTime.now(), "sku1", "sku2", "sku3");
        salesOrderUtil.createPersistedSalesOrder(salesOrder1.getOrderNumber(), LocalDateTime.now(), "sku4", "sku5");
        var orderNumber = salesOrder1.getOrderNumber();
        var event = ParcelShipped.builder()
                .orderNumber(orderNumber)
                .deliveryNoteNumber("delivery-note-12345")
                .trackingNumber("tracking-12345")
                .trackingLink("http://tacking-link")
                .logisticsPartnerName("dhl")
                .articleItemsDtos(Collections.singleton(
                        ArticleItemsDto.builder()
                                .number("sku1")
                                .quantity(BigDecimal.ONE)
                                .description("sku name 1")
                                .isDeposit(false)
                                .build()
                ))
                .build();

        salesOrderRowService.handleParcelShippedEvent(event);

        final var expectedSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder2.getOrderNumber());
        assertTrue(expectedSalesOrder.isPresent());

        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(
                argThat(
                        order -> {
                            assertThat(order.getOrderNumber()).isEqualTo(expectedSalesOrder.get().getOrderNumber());
                            assertThat(order.getOrderGroupId()).isEqualTo(expectedSalesOrder.get().getOrderGroupId());
                            return true;
                        }),
                argThat(
                        trackingLinks -> {
                            assertTrue(trackingLinks.stream().findFirst().isPresent());
                            assertThat(trackingLinks.stream().findFirst().get().getUrl()).isEqualTo(event.getTrackingLink());
                            var list = event.getArticleItemsDtos().stream().map(ArticleItemsDto::getNumber).collect(toList());
                            assertThat(trackingLinks.stream().findFirst().get().getOrderItems()).isEqualTo(list);
                            return true;
                        })
        );

    }

}
