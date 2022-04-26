package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kfzteile")
@Getter
@Setter
public class ProjectConfig {
    private Boolean deleteUnusedProcesses;
}
