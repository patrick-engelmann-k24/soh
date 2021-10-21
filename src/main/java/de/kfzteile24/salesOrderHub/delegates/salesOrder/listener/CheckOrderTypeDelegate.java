package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class CheckOrderTypeDelegate implements ExecutionListener {

    /**
     * The string is used to identify one of the sales channels listed in the following page
     * for garage and branch orders.
     *
     * <a href="https://kfzteile24.atlassian.net/wiki/spaces/IT/pages/2387019134/Sales+Channel+Code+mapping">
     *     Sales Channels at K24</a>
     */
    private final static String SALES_CHANNEL_BRANCH_ORDERS = "branch";

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        final String salesChannel = (String) delegateExecution.getVariable(Variables.SALES_CHANNEL.getName());
        boolean branchOrder = Optional.ofNullable(salesChannel)
                .map(s -> s.contains(SALES_CHANNEL_BRANCH_ORDERS))
                .orElse(false);

        log.info("The order type check has the following result. SalesChannel: {}, Is Branch Order: {}", salesChannel, branchOrder);
        delegateExecution.setVariable(Variables.IS_BRANCH_ORDER.getName(), branchOrder);
    }
}
