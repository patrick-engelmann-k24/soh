package com.kfzteile24.osh.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class MagicOrderDelegate implements JavaDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MagicOrderDelegate.class);

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        LOGGER.info("TODO: {}", delegateExecution);
    }
}
