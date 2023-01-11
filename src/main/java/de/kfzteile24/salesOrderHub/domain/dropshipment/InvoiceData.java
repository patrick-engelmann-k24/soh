package de.kfzteile24.salesOrderHub.domain.dropshipment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;

import javax.print.attribute.HashAttributeSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceData {
    private String invoiceNumber;
    private String orderNumber;
    private List<String> orderRows;
    private List<Integer> quantities;

    public Map<String, Integer> getSkuQuantityMap() {
        val result = new TreeMap<String, Integer>();
        for (int i = 0; i < orderRows.size(); i++) {
            val sku = orderRows.get(i);
            val quantity = quantities.get(i);
            result.put(sku, quantity);
        }
        return result;
    }
}