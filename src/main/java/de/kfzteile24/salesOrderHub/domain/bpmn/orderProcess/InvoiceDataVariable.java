package de.kfzteile24.salesOrderHub.domain.bpmn.orderProcess;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDataVariable {
    private String invoiceNumber;
    private String orderNumber;
    private boolean isPartialInvoice;
}