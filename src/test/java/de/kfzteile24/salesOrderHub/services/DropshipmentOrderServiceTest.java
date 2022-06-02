package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.constants.CurrencyType;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.FileUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentOrderServiceTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @InjectMocks
    @Spy
    private DropshipmentOrderService dropshipmentOrderService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SalesOrderReturnService salesOrderReturnService;

    @Mock
    private SalesOrderRowService salesOrderRowService;

    @ParameterizedTest
    @MethodSource("provideArgumentsForIsDropShipmentOrder")
    void testIsDropShipmentOrder(String fulfillment, boolean expected) {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(fulfillment);
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));
        assertThat(dropshipmentOrderService.isDropShipmentOrder(salesOrder.getOrderNumber())).isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    public void testHandleDropshipmentPurchaseOrderReturnConfirmed() {
        String rawMessage = readResource("examples/dropshipmentPurchaseOrderReturnConfirmed.json");
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentPurchaseOrderReturnConfirmedMessage message =
                objectMapper.readValue(body, DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        String orderNumber = message.getSalesOrderNumber();

        String rawSalesOrderMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        SalesOrder salesOrder = getSalesOrder(rawSalesOrderMessage);
        when(salesOrderService.getOrderByOrderNumber(eq(orderNumber))).thenReturn(Optional.of(salesOrder));
        when(salesOrderReturnService.createCreditNoteNumber()).thenReturn("2022200002");
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage = dropshipmentOrderService.buildSalesCreditNoteCreatedMessage(message);

        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber()).isEqualTo("2022200002");
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber()).isEqualTo(orderNumber);
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderGroupId()).isEqualTo(salesOrder.getOrderGroupId());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getCity()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getCity());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getCountryCode()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getCountryCode());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getStreet()).isEqualTo(Address.getStreet(salesOrder.getLatestJson().getOrderHeader().getBillingAddress()));
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getSalutation()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getSalutation());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getFirstName()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getFirstName());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getLastName()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getLastName());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getZipCode()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getZipCode());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCurrencyCode()).isEqualTo(CurrencyType.convert(salesOrder.getLatestJson().getOrderHeader().getOrderCurrency()));

        List<CreditNoteLine> creditNoteLines = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteLines().stream().collect(Collectors.toList());
        assertThat(creditNoteLines.size()).isEqualTo(2);
        assertThat(creditNoteLines.get(0).getIsShippingCost()).isEqualTo(false);
        assertThat(creditNoteLines.get(0).getQuantity()).isEqualTo(BigDecimal.valueOf(2));
        assertThat(creditNoteLines.get(0).getUnitNetAmount()).isEqualTo(BigDecimal.valueOf(10.84));
        assertThat(creditNoteLines.get(0).getLineNetAmount()).isEqualTo(BigDecimal.valueOf(21.68));
        assertThat(creditNoteLines.get(0).getItemNumber()).isEqualTo("2270-13013");
        assertThat(creditNoteLines.get(0).getLineTaxAmount()).isEqualTo(BigDecimal.valueOf(4.12));
        assertThat(creditNoteLines.get(0).getTaxRate()).isEqualTo(BigDecimal.valueOf(19.00).setScale(2));

        assertThat(creditNoteLines.get(1).getIsShippingCost()).isEqualTo(false);
        assertThat(creditNoteLines.get(1).getQuantity()).isEqualTo(BigDecimal.valueOf(2));
        assertThat(creditNoteLines.get(1).getUnitNetAmount()).isEqualTo(BigDecimal.valueOf(10.84));
        assertThat(creditNoteLines.get(1).getLineNetAmount()).isEqualTo(BigDecimal.valueOf(21.68));
        assertThat(creditNoteLines.get(1).getItemNumber()).isEqualTo("2270-13012");
        assertThat(creditNoteLines.get(1).getLineTaxAmount()).isEqualTo(BigDecimal.valueOf(4.12));
        assertThat(creditNoteLines.get(1).getTaxRate()).isEqualTo(BigDecimal.valueOf(19.00).setScale(2));

        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getNetAmount()).isEqualTo(BigDecimal.valueOf(43.36));
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getGrossAmount()).isEqualTo(BigDecimal.valueOf(51.60).setScale(2));

        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnConfirmed(message, salesCreditNoteCreatedMessage);
        verify(salesOrderRowService).handleSalesOrderReturn(salesCreditNoteCreatedMessage, DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED);
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return FileUtil.readResource(getClass(), path);
    }

    private static Stream<Arguments> provideArgumentsForIsDropShipmentOrder() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(StringUtils.EMPTY, false),
                Arguments.of(K24.getName(), false),
                Arguments.of(DELTICOM.getName(), true)
        );
    }
}