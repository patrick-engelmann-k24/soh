package de.kfzteile24.salesOrderHub.domain.pdh.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProductPartNumbers {
    String manufacturer;

    @JsonProperty("original_car_part_number")
    String partNumber;
}
