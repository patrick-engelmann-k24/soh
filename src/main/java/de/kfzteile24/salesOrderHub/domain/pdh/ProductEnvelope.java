package de.kfzteile24.salesOrderHub.domain.pdh;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;

@Data
@JsonRootName("product")
public class ProductEnvelope {

    private String version;

    private Product product;

}
