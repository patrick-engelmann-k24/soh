package de.kfzteile24.salesOrderHub.domain.dropshipment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceData {
    private String invoiceNumber;
    private String orderNumber;
    private List<String> orderRows;
}