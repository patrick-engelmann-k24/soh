package de.kfzteile24.salesOrderHub.converter;

import de.kfzteile24.salesOrderHub.dto.order.total.Taxes;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TotalTaxesConverter implements Converter<Taxes, GrandTotalTaxes> {

    @Override
    public GrandTotalTaxes convert(Taxes taxes) {
        return GrandTotalTaxes.builder()
                .rate(new BigDecimal(taxes.getRate()))
                .value(new BigDecimal(taxes.getValue()))
                .type(taxes.getType())
                .build();
    }
}