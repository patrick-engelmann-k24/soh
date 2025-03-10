package de.kfzteile24.salesOrderHub.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "kfzteile.dropshipment.splitgenarts")
@Data
public class DropShipmentConfig {

    @NotNull
    @NotEmpty
    private List<@NotBlank String> ecp;

    @NotNull
    @NotEmpty
    private List<@NotBlank String> deshop;
}
