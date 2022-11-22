package de.kfzteile24.salesOrderHub.constants.bpmn;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor
public enum ProcessDefinition {
    SALES_ORDER_PROCESS("SalesOrderProcess_v2"),
    SAVE_INVOICE_PROCESS("SaveInvoiceProcess"),
    RETURN_ORDER_PROCESS("ReturnOrderProcess"),
    INVOICE_CREATED_RECEIVED_PROCESS("InvoiceCreatedReceivedProcess"),
    INVOICING_PROCESS("InvoicingProcess")
    ;

    @NonNull
    private String name;

    public static ProcessDefinition fromName(String name) {
        return Arrays.stream(ProcessDefinition.values())
                .filter(processDefinition -> StringUtils.equals(processDefinition.getName(), name))
                .findFirst()
                .orElseThrow(() -> {
                    throw new IllegalArgumentException("Process definition not found based on name " + name);
                });
    }

}
