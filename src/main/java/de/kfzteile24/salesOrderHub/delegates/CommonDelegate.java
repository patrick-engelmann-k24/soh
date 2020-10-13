package de.kfzteile24.salesOrderHub.delegates;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;

abstract public class CommonDelegate implements JavaDelegate {

    @Autowired
    protected
    CamundaHelper helper;

    protected void setResultVariable(DelegateExecution delegateExecution, BpmItem varName, boolean checkResult) {
        delegateExecution.setVariable(varName.getName(), checkResult);
    }
}
