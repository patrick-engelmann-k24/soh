package de.kfzteile24.salesOrderHub.configuration.process;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class SalesOrderProcess {
    private String dropshipmentOrderBookedTimer;
    private String dropshipmentShipmentConfirmedTimer;
}
