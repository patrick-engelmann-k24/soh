package de.kfzteile24.salesOrderHub.domain.pdh.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Country {

    private List<String> ean;

    private List<String> genart;

    @JsonProperty("product_identifier")
    private void unpackNameFromNestedObject(Map<String, List<String>> map) {
        ean = map.get("eans");
        genart = map.get("genart_numbers");

    }
}
