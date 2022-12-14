package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
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
        doReturn(List.of(salesOrder)).when(adaptor).getSalesOrderList(any(), any());

        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage =
                returnOrderHelper.buildSalesCreditNoteCreatedMessage(message, salesOrder, "2022200002");

        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber()).isEqualTo("2022200002");
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber()).isEqualTo(orderNumber);
        assertSalesCreditNoteCreatedMessage(salesCreditNoteCreatedMessage, salesOrder);
    }
}