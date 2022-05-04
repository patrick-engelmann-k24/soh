package de.kfzteile24.salesOrderHub.constants.bpmn;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor
public enum ProcessDefinition {
    SALES_ORDER_PROCESS("SalesOrderProcess_v2"),
    SALES_ORDER_ROW_FULFILLMENT_PROCESS("OrderRowFulfillmentProcess"),
    SAVE_INVOICE_PROCESS("SaveInvoiceProcess"),
    RETURN_ORDER_PROCESS("ReturnOrderProcess"),
    INVOICE_RECEIVED_PROCESS("InvoiceReceivedProcess")
    ;

    @NonNull
    private String name;

}
