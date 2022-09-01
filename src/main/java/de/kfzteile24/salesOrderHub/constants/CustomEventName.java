package de.kfzteile24.salesOrderHub.constants;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor
public enum CustomEventName {
    SALES_ORDER_CONSUMED("SohSalesOrderConsumed"),
    SPLIT_ORDER_GENERATED("SohSplitOrderGenerated"),
    SUBSEQUENT_ORDER_GENERATED("SohSubsequentOrderGenerated"),
    SALES_ORDER_PUBLISHED("SohSalesOrderPublished"),
    CORE_INVOICE_PUBLISHED("SohCoreInvoicePublished"),
    DROPSHIPMENT_INVOICE_PUBLISHED("SohDropshipmentInvoicePublished"),
    DROPSHIPMENT_ORDER_BOOKED_MISSING("DropshipmentOrderBookedMissing"),
    DROPSHIPMENT_SHIPMENT_CONFIRMED_MISSING("DropshipmentShipmentConfirmedMissing");
    @NonNull
    private String name;
}
