package de.kfzteile24.salesOrderHub.dto.sns.paymentsecured;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderData {

    @JsonProperty("OrderGroupId")
    private String orderGroupId;

    @Singular("salesOrderId")
    @JsonProperty("SalesOrderId")
    private Collection<String> salesOrderId;

}
