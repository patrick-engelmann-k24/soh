package de.kfzteile24.salesOrderHub.domain.dropshipment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceData {
    private String invoiceNumber;
    private String orderNumber;
    private List<String> orderRows;
    private List<Pair<String, Integer>> orderRowAndQuantity;
}