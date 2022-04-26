package de.kfzteile24.salesOrderHub.delegates.returnorder;

import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
class PublishReturnOrderCreatedDelegate implements JavaDelegate {

    private final SnsPublishService snsPublishService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("{} delegate invoked", PublishReturnOrderCreatedDelegate.class.getSimpleName());
    }
}
