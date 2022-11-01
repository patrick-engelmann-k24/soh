package de.kfzteile24.salesOrderHub.domain.audit;

import de.kfzteile24.salesOrderHub.domain.converter.AuditActionConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Deprecated actions remain as they are,
 * since they will be still used for the enum converting from existing entries in the audit_log table
 *
 * @see AuditActionConverter
 */
@RequiredArgsConstructor
public enum Action {
    ORDER_CREATED("ORDER_CREATED"),
    ORDER_PAYMENT_SECURED("ORDER_PAYMENT_SECURED"),
    ORDER_CANCELLED("ORDER_CANCELLED"),
    ORDER_COMPLETED("ORDER_COMPLETED"),
    INVOICE_RECEIVED("INVOICE_RECEIVED"),
    INVOICE_ADDRESS_CHANGED("INVOICE_ADDRESS_CHANGED"),
    @Deprecated(since = "30.10.2022", forRemoval = true)
    DELIVERY_ADDRESS_CHANGED("DELIVERY_ADDRESS_CHANGED"),
    @Deprecated(since = "30.10.2022", forRemoval = true)
    ORDER_ITEM_TOUR_STARTED("ORDER_ITEM_TOUR_STARTED"),
    @Deprecated(since = "30.10.2022", forRemoval = true)
    ORDER_ITEM_TRACKING_ID_RECEIVED("ORDER_ITEM_TRACKING_ID_RECEIVED"),
    @Deprecated(since = "30.10.2022", forRemoval = true)
    ORDER_ITEM_PACKING_STARTED("ORDER_ITEM_PACKING_STARTED"),
    @Deprecated(since = "30.10.2022", forRemoval = true)
    ORDER_ITEM_TRANSMITTED_TO_LOGISTIC("ORDER_ITEM_TRANSMITTED_TO_LOGISTIC"),
    ORDER_ITEM_SHIPPED("ORDER_ITEM_SHIPPED"),
    ORDER_ROW_CANCELLED("ORDER_ROW_CANCELLED"),
    DROPSHIPMENT_PURCHASE_ORDER_BOOKED("DROPSHIPMENT_PURCHASE_ORDER_BOOKED"),
    DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED("DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED"),
    RETURN_ORDER_CREATED("RETURN_ORDER_CREATED"),
    DROPSHIPMENT_INVOICE_STORED("DROPSHIPMENT_INVOICE_STORED"),
    MIGRATION_SALES_ORDER_RECEIVED("MIGRATION_SALES_ORDER_RECEIVED");

    @Getter
    private final String action;
}
