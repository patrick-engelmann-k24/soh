package de.kfzteile24.salesOrderHub.domain.pdh.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Localization {

    private String name;
    private String genart;

    @JsonProperty("internal_designation")
    void unpackNameFromNestedObject(Map<String, String> map) {
        name = map.get("designation1");
    }

    @JsonProperty("genarts")
    void unpackGenartNameFromNestedObject(List<Map<String, String>> list) {
        genart = CollectionUtils.isEmpty(list) ? null : list.get(0).get("genart_name");
    }
}
