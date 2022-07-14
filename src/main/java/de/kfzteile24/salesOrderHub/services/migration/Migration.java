package de.kfzteile24.salesOrderHub.services.migration;

import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Migration {

    ProcessDefinition processDefinition();
    int version();
}
