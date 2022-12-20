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
    DROPSHIPMENT_SHIPMENT_CONFIRMED_MISSING("DropshipmentShipmentConfirmedMissing"),
    DROPSHIPMENT_ORDER_CREATED("DropshipmentOrderCreated"),
    DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED("DropshipmentSubsequentOrderCreated"),
    DROPSHIPMENT_ORDER_CONFIRMED("DropshipmentOrderConfirmed"),
    DROPSHIPMENT_ORDER_CANCELLED("DropshipmentOrderCancelled"),
    DROPSHIPMENT_ORDER_FULLY_INVOICED("DropshipmentOrderFullyInvoiced"),
    DROPSHIPMENT_ORDER_RETURN_NOTIFIED("DropshipmentOrderReturnNotified"),
    DROPSHIPMENT_ORDER_RETURN_CONFIRMED("DropshipmentOrderReturnConfirmed"),
    DROPSHIPMENT_ORDER_RETURN_CREATED("DropshipmentOrderReturnCreated"),
    DROPSHIPMENT_ORDER_CREDIT_NOTE_CREATED("DropshipmentOrderCreditNoteCreated");

    @NonNull
    private String name;
}
