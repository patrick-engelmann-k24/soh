package de.kfzteile24.salesOrderHub.dto.order.riskcheck;

import lombok.Data;

@Data
public class TrustNPayScore {
    private String communicationToken;
    private String result;
}
