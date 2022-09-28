package de.kfzteile24.salesOrderHub.services.processmigration;

import de.kfzteile24.salesOrderHub.services.processmigration.exception.NoProcessDefinitionFound;
import de.kfzteile24.salesOrderHub.services.processmigration.exception.NoVariableInstanceFound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toUnmodifiableList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessQueryService {

    private final ProcessEngine processEngine;

    public List<ProcessInstance> getProcessInstancesIdsByProcessDefinitionId(String processDefinitionId) {
        return processEngine.getRuntimeService().createProcessInstanceQuery()
                .processDefinitionId(processDefinitionId)
                .list()
                .stream()
                .collect(toUnmodifiableList());
    }

    public ProcessInstance getAnyExecutionByProcessDefinitionId(String processDefinitionId) {
        return processEngine.getRuntimeService().createProcessInstanceQuery()
                .processDefinitionId(processDefinitionId)
                .listPage(0, 1)
                .stream()
                .findAny()
                .orElse(null);
    }

    public ProcessInstance getProcessInstance(String processInstanceId) {
        return processEngine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    public String getProcessDefinitionId(String processDefinition, int version) {
        return Optional.ofNullable(processEngine.getRepositoryService().createProcessDefinitionQuery()
                .processDefinitionKey(processDefinition)
                .processDefinitionVersion(version)
                .singleResult())
                .map(ProcessDefinition::getId)
                .orElseThrow(() -> new NoProcessDefinitionFound(format("{0}:{1}", processDefinition, version)));
    }

    public ProcessDefinition getProcessDefinition(String processDefinitionId) {
        return Optional.ofNullable(processEngine.getRepositoryService().createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult())
                .orElseThrow(() -> new NoProcessDefinitionFound(processDefinitionId));
    }

    public int getProcessDefinitionLastVersion(String processDefinition) {
        return Optional.ofNullable(processEngine.getRepositoryService().createProcessDefinitionQuery()
                .processDefinitionKey(processDefinition)
                .latestVersion()
                .singleResult())
                .map(ProcessDefinition::getVersion)
                .orElseThrow(() -> new NoProcessDefinitionFound(processDefinition));
    }

    public ProcessDefinition getLastProcessDefinition(String processDefinition) {
        return Optional.ofNullable(processEngine.getRepositoryService().createProcessDefinitionQuery()
                .processDefinitionKey(processDefinition)
                .singleResult())
                .orElseThrow(() -> new NoProcessDefinitionFound(processDefinition));
    }

    @SuppressWarnings("unchecked")
    public <T> T getVariableValue(List<VariableInstance> variableInstances, String processVariableName) {
        return (T) variableInstances.stream()
                .filter(processVariable -> StringUtils.equals(processVariable.getName(), processVariableName))
                .findFirst()
                .map(VariableInstance::getValue)
                .orElseThrow(() -> new NoVariableInstanceFound(processVariableName));
    }

    public List<VariableInstance> getVariableInstances(String processInstanceId) {
        return processEngine.getRuntimeService().createVariableInstanceQuery()
                .processInstanceIdIn(processInstanceId)
                .list();
    }
}
