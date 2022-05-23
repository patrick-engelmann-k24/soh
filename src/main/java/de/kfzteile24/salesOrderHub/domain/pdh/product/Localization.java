package de.kfzteile24.salesOrderHub.domain.pdh.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Localization {

    private String name;

    @JsonProperty("internal_designation")
    private void unpackNameFromNestedObject(Map<String, String> map) {
        name = map.get("designation1");
    }
}
