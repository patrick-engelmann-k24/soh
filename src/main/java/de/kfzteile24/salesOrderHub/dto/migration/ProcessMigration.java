package de.kfzteile24.salesOrderHub.dto.migration;

import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessMigration {

    private ProcessDefinition processDefinition;
    private int version;

    @Override
    public String toString() {
        return "ProcessMigration{" +
                "processDefinition=" + processDefinition +
                ", version=" + version +
                '}';
    }
}
