package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
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
                .build();
    }
}
