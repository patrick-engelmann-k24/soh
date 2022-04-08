package de.kfzteile24.salesOrderHub.dto.sns.paymentsecured;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;

@Builder
@Value
@Jacksonized
public class OrderData {

    @JsonProperty("OrderGroupId")
    String orderGroupId;

    @Singular("salesOrderId")
    @JsonProperty("SalesOrderId")
    Collection<String> salesOrderId;

}
