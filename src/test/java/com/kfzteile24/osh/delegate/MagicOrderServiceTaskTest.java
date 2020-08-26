package com.kfzteile24.osh.delegate;

import com.kfzteile24.osh.SohProcessApplication;
import com.kfzteile24.osh.helper.BpmnRuleTestHelper;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SohProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class MagicOrderServiceTaskTest {
    @Autowired
    ProcessEngine processEngine;

    @Rule
    public ProcessEngineRule processEngineRule = new BpmnRuleTestHelper();

    @Test
    @Deployment(resources = {"demo.bpmn"})
    public void ruleUsageExample() {
        RuntimeService runtimeService = processEngineRule.getRuntimeService();
        runtimeService.startProcessInstanceByKey("Demo_SOH_process");

        TaskService taskService = processEngineRule.getTaskService();
        Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Somebody is doing a hard job", task.getName());

        taskService.complete(task.getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }


}
