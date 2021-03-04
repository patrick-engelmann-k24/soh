package de.kfzteile24.salesOrderHub.components;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.DiagramLayout;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;

@Component
public class CheckEventNames {

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    ProcessEngine processEngine;

    @PostConstruct
    void checkEventNames() {

        List<ProcessDefinition> processDefinitions = getProcessDefinitons();

        for (ProcessDefinition processDefinition : processDefinitions) {
            checkEvents(processDefinition);
        }

    }

    List<ProcessDefinition> getProcessDefinitons() {
        return repositoryService.createProcessDefinitionQuery().latestVersion().active().list();
    }

    void checkEvents(ProcessDefinition processDefinition) {
        DiagramLayout processDiagramLayout = repositoryService.getProcessDiagramLayout(processDefinition.getId());
        InputStream processDiagram = repositoryService.getProcessDiagram(processDefinition.getId());
        ProcessDefinition processDefinition1 = repositoryService.getProcessDefinition(processDefinition.getId());
        List<Task> list = processEngine.getTaskService().createTaskQuery().processDefinitionId(processDefinition.getId()).list();
        System.out.println(processDefinition.getId());
    }


}
