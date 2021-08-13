package de.kfzteile24.salesOrderHub.converter;

import de.kfzteile24.soh.order.dto.CustomerType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CustomerTypeConverter implements Converter<String, CustomerType> {

    @Override
    public CustomerType convert(String customerType) {
        return CustomerType.getCustomerByType(customerType);
    }
}
