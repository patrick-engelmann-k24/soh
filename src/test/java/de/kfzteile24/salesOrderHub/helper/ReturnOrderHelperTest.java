package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.services.returnorder.ReturnOrderServiceAdaptor;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.assertSalesCreditNoteCreatedMessage;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ReturnOrderHelperTest {

    @Mock
    private ReturnOrderServiceAdaptor adaptor;
    @InjectMocks
    @Spy
    private ReturnOrderHelper returnOrderHelper;

    @Test
    @SneakyThrows
    void testBuildSalesCreditNoteCreatedMessage() {
        var message = getObjectByResource("dropshipmentPurchaseOrderReturnConfirmed.json", DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        String orderNumber = message.getSalesOrderNumber();
        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class));
        SalesOrder salesOrder1 = getSalesOrder(getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class));
        Order orderJson1 = salesOrder1.getLatestJson();
        orderJson1.getOrderHeader().setOrderNumber(salesOrder.getOrderNumber() + "-1");
        salesOrder1.setOrderNumber(orderJson1.getOrderHeader().getOrderNumber());
        orderJson1.getOrderRows().get(0).setSku("2270-13013");
        orderJson1.getOrderRows().get(1).setSku("2270-13015");
        SalesOrder salesOrder2 = getSalesOrder(getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class));
        Order orderJson2 = salesOrder2.getLatestJson();
        orderJson2.getOrderHeader().setOrderNumber(salesOrder.getOrderNumber() + "-2");
        salesOrder2.setOrderNumber(orderJson2.getOrderHeader().getOrderNumber());
        orderJson2.getOrderRows().get(0).setSku("2270-13012");
        orderJson2.getOrderRows().get(1).setSku("2270-13014");

        doReturn(List.of(salesOrder1, salesOrder2)).when(adaptor).getSalesOrderList(any(), any());
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage =
                returnOrderHelper.buildSalesCreditNoteCreatedMessage(message, salesOrder, "2022200002");

        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber()).isEqualTo("2022200002");
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber()).isEqualTo(orderNumber);
        assertSalesCreditNoteCreatedMessage(salesCreditNoteCreatedMessage, salesOrder);
    }

    @Test
    @SneakyThrows
    void testBuildSalesCreditNoteCreatedMessageThrowException() {
        var message = getObjectByResource("dropshipmentPurchaseOrderReturnConfirmed.json", DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        String orderNumber = message.getSalesOrderNumber();
        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class));
        salesOrder.getLatestJson().getOrderRows().get(0).setSku("2270-13014");
        salesOrder.setOrderGroupId(orderNumber);

        doReturn(List.of(salesOrder)).when(adaptor).getSalesOrderList(any(), any());
        assertThatThrownBy(() -> returnOrderHelper.buildSalesCreditNoteCreatedMessage(message, salesOrder, "2022200002"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("The skus, 2270-13013 are missing in received" +
                        " dropshipment purchase order return confirmed message with" +
                        " Sales Order Group Id: " + orderNumber + "," +
                        " External Order Number: " + message.getExternalOrderNumber());
    }
}