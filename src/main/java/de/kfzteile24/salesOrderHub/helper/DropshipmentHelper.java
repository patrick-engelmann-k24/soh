package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DropshipmentHelper {

    public DropshipmentInvoiceRow createDropshipmentInvoiceRow(String sku, String orderNumber) {

        return DropshipmentInvoiceRow.builder()
                .sku(sku)
                .orderNumber(orderNumber)
                .quantity(0) //TODO: implement quantity
                .build();
    }

    public DropshipmentInvoiceRow createDropshipmentInvoiceRow(String sku, String orderNumber, String invoiceNumber) {

        return DropshipmentInvoiceRow.builder()
                .sku(sku)
                .orderNumber(orderNumber)
                .invoiceNumber(invoiceNumber)
                .quantity(0) //TODO: implement quantity
                .build();
    }

    public DropshipmentOrderRow createDropshipmentOrderRow(String sku, String orderNumber) {

        return DropshipmentOrderRow.builder()
                .sku(sku)
                .orderNumber(orderNumber)
                .quantity(0) //TODO: implement quantity
                .quantityShipped(0)
                .build();
    }
}
