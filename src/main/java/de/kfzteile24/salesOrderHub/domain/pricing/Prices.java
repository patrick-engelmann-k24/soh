package de.kfzteile24.salesOrderHub.domain.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Prices {

    private BigDecimal net;

    private BigDecimal gross;
}
