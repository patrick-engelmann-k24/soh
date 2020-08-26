package com.kfzteile24.osh.delegate;

import org.camunda.bpm.engine.delegate.*;
import org.camunda.bpm.extension.mockito.DelegateExpressions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.camunda.bpm.engine.delegate.TaskListener.EVENTNAME_CREATE;
import static org.camunda.bpm.extension.mockito.CamundaMockito.candidateGroupIds;
import static org.camunda.bpm.extension.mockito.CamundaMockito.delegateTaskFake;
import static org.camunda.bpm.model.xml.test.assertions.ModelAssertions.assertThat;
import static org.mockito.Mockito.mock;

public class DelegateTest {
    private static final String BEAN_NAME = "magicOrderDelegate";
    private static final String MESSAGE = "message";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void taskListenerSetsCandidateGroup() throws Exception {
        TaskListener taskListener = task -> {
            if (EVENTNAME_CREATE.equals(task.getEventName()) && "execute MagicOrderThingie".equals(task.getTaskDefinitionKey())) {
                task.addCandidateGroup((String) task.getVariableLocal("nextGroup"));
            }
        };

        // given a delegateTask
        DelegateTask delegateTask = delegateTaskFake()
                .withTaskDefinitionKey("execute MagicOrderThingie")
                .withEventName(EVENTNAME_CREATE)
                .withVariableLocal("nextGroup", "foo");

        // when
        taskListener.notify(delegateTask);

        // then the candidate group was set
        assertThat(candidateGroupIds(delegateTask)).containsOnly("foo");

    }


    @Test
    public void shouldThrowBpmnError() throws Exception {

        // expect exception
        thrown.expect(BpmnError.class);
        thrown.expectMessage(MESSAGE);

        DelegateExpressions.registerJavaDelegateMock(BEAN_NAME).onExecutionThrowBpmnError("code", MESSAGE);

        final JavaDelegate registeredDelegate = DelegateExpressions.getJavaDelegateMock(BEAN_NAME);

        // test succeeds when exception is thrown
        registeredDelegate.execute(mock(DelegateExecution.class));
    }
}
