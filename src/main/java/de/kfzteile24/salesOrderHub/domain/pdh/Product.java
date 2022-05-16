package de.kfzteile24.salesOrderHub.domain.pdh;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.domain.pdh.product.ProductPartNumbers;
import de.kfzteile24.salesOrderHub.domain.pdh.product.ProductSet;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Product {

    @JsonProperty(value = "pdm_product_id")
    String productId;

    @JsonProperty("k24_sku")
    String sku;

    @JsonProperty("genart_numbers")
    List<String> genartNumberList;

    @JsonProperty("set_products")
    List<ProductSet> setProductCollection;

    // todo: what is the right name field?
    String name = "foo";

    @JsonProperty("ean")
    Map<String, List<String>> eanMap;

    @JsonProperty("original_part_numbers")
    List<ProductPartNumbers> partNumbers;

    public boolean isSetItem() {
        if(setProductCollection == null) {
            return false;
        }

        return setProductCollection.size() != 0;
    }
}
