package de.kfzteile24.salesOrderHub.domain.pdh;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;

@Data
@JsonRootName("product")
public class ProductEnvelope {

    String version;

    Product product;

}
