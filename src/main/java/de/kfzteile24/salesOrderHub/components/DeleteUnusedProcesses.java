package de.kfzteile24.salesOrderHub.components;

import de.kfzteile24.salesOrderHub.configuration.ProjectConfiguration;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;

// disabled, cause to slow
//@Component
@Log
public class DeleteUnusedProcesses {

    @Autowired
    private ProjectConfiguration projectConfiguration;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    /**
     * Remove old process instances that have no active processes anymore.
     */
    @PostConstruct
    public void deleteUnusedDeployments() {
        if (!projectConfiguration.getDeleteUnusedProcesses()) {
            log.info("Remove old processes disabled. Aborting here.");
            return;
        }

        log.info("Checking if old processes can be deleted");
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            boolean deploymentCanBeDeleted = true;

            List<ProcessDefinition> processDefinitions = repositoryService
                    .createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).list();
            for (ProcessDefinition processDefinition : processDefinitions) {
                ProcessDefinition latestProcessDefiniton = repositoryService
                        .createProcessDefinitionQuery()
                        .processDefinitionKey(processDefinition.getKey())
                        .latestVersion().singleResult();
                boolean isLatest = latestProcessDefiniton.getId().equals(processDefinition.getId());
                boolean hasRunningInstances = runtimeService
                        .createProcessInstanceQuery()
                        .processDefinitionId(processDefinition.getId()).count() > 0;
                boolean hasHistoricInstances = historyService
                        .createHistoricProcessInstanceQuery()
                        .processDefinitionId(processDefinition.getId()).count() > 0;
                if (isLatest || hasRunningInstances || hasHistoricInstances) {
                    deploymentCanBeDeleted = false;
                    break;
                }
            }

            if (deploymentCanBeDeleted) {
                log.info("Remove process: " + deployment.getId() + " - " + deployment.getName());
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

}
