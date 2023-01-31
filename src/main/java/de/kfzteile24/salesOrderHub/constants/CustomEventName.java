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
    DROPSHIPMENT_ORDER_BOOKED_MISSING("SohDropshipmentOrderBookedMissing"),
    DROPSHIPMENT_SHIPMENT_CONFIRMED_MISSING("SohDropshipmentShipmentConfirmedMissing"),
    DROPSHIPMENT_ORDER_CREATED("SohDropshipmentOrderCreated"),
    DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED("SohDropshipmentSubsequentOrderCreated"),
    DROPSHIPMENT_ORDER_CONFIRMED("SohDropshipmentOrderConfirmed"),
    DROPSHIPMENT_ORDER_CANCELLED("SohDropshipmentOrderCancelled"),
    DROPSHIPMENT_ORDER_FULLY_INVOICED("SohDropshipmentOrderFullyInvoiced"),
    DROPSHIPMENT_ORDER_RETURN_NOTIFIED("SohDropshipmentOrderReturnNotified"),
    DROPSHIPMENT_ORDER_RETURN_CREATED("SohDropshipmentOrderReturnCreated"),
    DROPSHIPMENT_ORDER_CREDIT_NOTE_CREATED("SohDropshipmentOrderCreditNoteCreated");

    @NonNull
    private String name;
}
