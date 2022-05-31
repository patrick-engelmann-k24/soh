package de.kfzteile24.salesOrderHub.configuration;

import de.kfzteile24.salesOrderHub.configuration.process.ProcessConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "kfzteile")
@Getter
@Setter
public class ProjectConfig {
    private Boolean deleteUnusedProcesses;
    private ProcessConfig processConfig;
    private List<PersistentPropertyConfig> persistentProperties;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersistentPropertyConfig {
        private String key;
        private Object value;
        private Boolean overwriteOnStartup;
    }
}
