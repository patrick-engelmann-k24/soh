package de.kfzteile24.salesOrderHub.dto.order.riskcheck;

import lombok.Data;

@Data
public class RiskCheck {
    TrustNPayScore trustNPayScore;
    String levenshteinScore;
}
