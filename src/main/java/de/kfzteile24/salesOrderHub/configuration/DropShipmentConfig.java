package de.kfzteile24.salesOrderHub.configuration;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "kfzteile")
@Data
public class DropShipmentConfig {

    @NotNull
    @NotEmpty
    private Boolean deleteUnusedProcesses;

    @NotNull
    @NotEmpty
    private List<@NotBlank String> dropShipmentSplitGenarts;
}
