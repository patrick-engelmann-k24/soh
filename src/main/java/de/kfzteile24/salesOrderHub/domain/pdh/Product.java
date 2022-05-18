package de.kfzteile24.salesOrderHub.domain.pdh;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.domain.pdh.product.Country;
import de.kfzteile24.salesOrderHub.domain.pdh.product.Localization;
import de.kfzteile24.salesOrderHub.domain.pdh.product.ProductPartNumbers;
import de.kfzteile24.salesOrderHub.domain.pdh.product.ProductSet;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Product {

    @JsonProperty("k24_sku")
    private String sku;

    @JsonProperty("set_products")
    private List<ProductSet> setProductCollection;

    @JsonProperty("localizations")
    private Map<String, Localization> localizations;

    @JsonProperty("countries")
    private Map<String, Country> countries;

    @JsonProperty("original_part_numbers")
    private List<ProductPartNumbers> partNumbers;

    public boolean isSetItem() {
        if (setProductCollection == null) {
            return false;
        }

        return setProductCollection.size() != 0;
    }
}
