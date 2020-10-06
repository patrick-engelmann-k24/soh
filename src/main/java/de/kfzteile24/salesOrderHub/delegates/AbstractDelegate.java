package de.kfzteile24.salesOrderHub.delegates;

import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

@Log
abstract public class AbstractDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // todo: remove after implementation of all delegates.
        log.warning("This delegate is not yet implemented");
    }
}
