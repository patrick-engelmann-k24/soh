package de.kfzteile24.salesOrderHub.helper;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

public class BpmnRuleTestHelper extends ProcessEngineRule {
    @Override
    public Statement apply(Statement base, Description description) {
        // you can use this, if you use AbstractAssertions
        // super.processEngine = AbstractAssertions.processEngine();
        super.processEngine = processEngine();
        return super.apply(base, description);
    }

    public static ProcessEngine processEngine() {
        Map<String, ProcessEngine> processEngines = ProcessEngines.getProcessEngines();
        if (processEngines.size() == 1) {
            return processEngines.values().iterator().next();
        } else {
            throw new IllegalStateException("unable to obtain a process engine");
        }
    }
}
