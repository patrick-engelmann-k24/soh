package de.kfzteile24.salesOrderHub.modeltests;

import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.DropshipmentCreateUpdateShipmentDataDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.DropshipmentOrderCancellationDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.DropshipmentOrderRowsCancellationDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.PublishDropshipmentItemShipmentCompletedDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.PublishDropshipmentOrderCreatedDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.PublishDropshipmentOrderRowTrackingInformationDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.SaveDropshipmentOrderItemsDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener.CheckIsDropshipmentOrderListener;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener.CheckProcessingDropshipmentOrderListener;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener.NewRelicAwareTimerListener;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.delegates.invoicing.AggregateInvoiceDataDelegate;
import de.kfzteile24.salesOrderHub.delegates.invoicing.CreateDropshipmentSubsequentInvoiceDelegate;
import de.kfzteile24.salesOrderHub.delegates.invoicing.CreateDropshipmentSubsequentOrderDelegate;
import de.kfzteile24.salesOrderHub.delegates.invoicing.DetermineDropshipmentOrderInvoiceTypeDelegate;
import de.kfzteile24.salesOrderHub.delegates.invoicing.DropshipmentOrderFullyInvoicedDelegate;
import de.kfzteile24.salesOrderHub.delegates.invoicing.DropshipmentOrderGenerateInvoicePdfDelegate;
import de.kfzteile24.salesOrderHub.delegates.invoicing.DropshipmentOrderPublishInvoiceDataDelegate;
import de.kfzteile24.salesOrderHub.delegates.invoicing.DropshipmentOrderStoreInvoiceDelegate;
import de.kfzteile24.salesOrderHub.delegates.invoicing.StartDropshipmentSubsequentOrderProcessDelegate;
import de.kfzteile24.salesOrderHub.delegates.returnorder.PublishCreditNoteReceivedDelegate;
import de.kfzteile24.salesOrderHub.delegates.returnorder.PublishReturnOrderCreatedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.CancelDropshipmentOrderDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.CancelOrderDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.ChangeInvoiceAddressDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.ChangeInvoiceAddressPossibleDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.CleanupDropshipmentInvoiceRowTableDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.InvoiceAddressChangedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCancelledDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCompletedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCreatedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.listener.CheckOrderTypeDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.listener.CheckPaymentTypeDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.listener.CheckPlatformTypeDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.row.OrderRowCancelledDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Payments;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.extension.process_test_coverage.spring_test.ProcessEngineCoverageConfiguration;
import org.camunda.bpm.extension.process_test_coverage.spring_test.ProcessEngineCoverageTestExecutionListener;
import org.camunda.bpm.scenario.ProcessScenario;
import org.camunda.bpm.scenario.Scenario;
import org.camunda.bpm.scenario.act.EventBasedGatewayAction;
import org.camunda.bpm.scenario.act.MessageIntermediateCatchEventAction;
import org.camunda.bpm.scenario.act.ReceiveTaskAction;
import org.camunda.bpm.scenario.act.SignalIntermediateCatchEventAction;
import org.camunda.bpm.scenario.delegate.EventBasedGatewayDelegate;
import org.camunda.bpm.scenario.delegate.EventSubscriptionDelegate;
import org.camunda.bpm.scenario.impl.ProcessRunnerImpl;
import org.camunda.bpm.scenario.run.Runner;
import org.camunda.bpm.spring.boot.starter.CamundaBpmConfiguration;
import org.camunda.bpm.spring.boot.starter.SpringBootProcessApplication;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.RECURRING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.CUSTOMER_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PLATFORM_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_CHANNEL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.VIRTUAL_ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.VOUCHER;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        CoverageTestConfiguration.class,
        ProcessEngineCoverageConfiguration.class,
        CamundaBpmConfiguration.class
})
@TestExecutionListeners(value = ProcessEngineCoverageTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@MockBean({
        PublishReturnOrderCreatedDelegate.class,
        PublishCreditNoteReceivedDelegate.class,
        CheckIsDropshipmentOrderListener.class,
        CheckProcessingDropshipmentOrderListener.class,
        DropshipmentOrderRowsCancellationDelegate.class,
        DropshipmentOrderCancellationDelegate.class,
        OrderCreatedDelegate.class,
        OrderCancelledDelegate.class,
        PublishDropshipmentOrderCreatedDelegate.class,
        DropshipmentOrderPublishInvoiceDataDelegate.class,
        DropshipmentOrderStoreInvoiceDelegate.class,
        OrderCompletedDelegate.class,
        SimpleMessageListenerContainer.class,
        KeyValuePropertyService.class,
        SpringBootProcessApplication.class,
        ChangeInvoiceAddressPossibleDelegate.class,
        ChangeInvoiceAddressDelegate.class,
        InvoiceAddressChangedDelegate.class,
        CheckOrderTypeDelegate.class,
        CheckPaymentTypeDelegate.class,
        CheckPlatformTypeDelegate.class,
        CancelOrderDelegate.class,
        PublishDropshipmentOrderRowTrackingInformationDelegate.class,
        DropshipmentCreateUpdateShipmentDataDelegate.class,
        AggregateInvoiceDataDelegate.class,
        DetermineDropshipmentOrderInvoiceTypeDelegate.class,
        CreateDropshipmentSubsequentOrderDelegate.class,
        StartDropshipmentSubsequentOrderProcessDelegate.class,
        CreateDropshipmentSubsequentInvoiceDelegate.class,
        CancelDropshipmentOrderDelegate.class,
        DropshipmentOrderGenerateInvoicePdfDelegate.class,
        DropshipmentOrderFullyInvoicedDelegate.class,
        CleanupDropshipmentInvoiceRowTableDelegate.class,
        OrderRowCancelledDelegate.class,
        PublishDropshipmentItemShipmentCompletedDelegate.class,
        SaveDropshipmentOrderItemsDelegate.class
})
public abstract class AbstractWorkflowTest implements ApplicationContextAware {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AbstractWorkflowTest.class);

    public static final SignalIntermediateCatchEventAction WAIT_SIGNAL_CATCH_EVENT_ACTION = action -> {};
    public static final MessageIntermediateCatchEventAction WAIT_MESSAGE_CATCH_EVENT_ACTION = action -> {};
    public static final ReceiveTaskAction WAIT_RECEIVER_TASK_ACTION = action -> {};
    public static final MessageIntermediateCatchEventAction RECEIVED_MESSAGE_CATCH_EVENT_ACTION =
            EventSubscriptionDelegate::receive;
    public static final ReceiveTaskAction RECEIVED_RECEIVER_TASK_ACTION =
            EventSubscriptionDelegate::receive;
    public static final SignalIntermediateCatchEventAction RECEIVED_SIGNAL_CATCH_EVENT_ACTION =
            EventSubscriptionDelegate::receive;
    public static final EventBasedGatewayAction RECEIVED_SIGNAL_EVENT_GATEWAY_ACTION =
            EventBasedGatewayDelegate::getEventSubscriptions;

    protected static ProcessEngine processEngine;

    protected static ApplicationContext applicationContext;

    @Mock
    protected ProcessScenario processScenario;

    @SpyBean
    protected NewRelicAwareTimerListener newRelicAwareTimerListener;

    @MockBean
    protected MetricsHelper metricsHelper;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected CamundaHelper camundaHelper;

    protected Map<String, Object> processVariables;

    protected String businessKey;

    /**
     * The application context gets created only once for all the model tests
     */
    @Override
    @SneakyThrows
    public void setApplicationContext(ApplicationContext newApplicationContext) {
        if (Objects.isNull(applicationContext)) {
            applicationContext = newApplicationContext;
            LOGGER.info(" ========== Application context created ==========");
        }
    }

    /**
     * The process engine gets created only once for all the model tests
     */
    @BeforeEach
    @SneakyThrows
    protected void setUp() {
        if (Objects.isNull(processEngine)) {
            processEngine = applicationContext.getBean(ProcessEngine.class);
            LOGGER.info(" ========== Default process engine created ==========");
        }
        LOGGER.info("Process engine name: {}. Object id: {}", processEngine.getName(), processEngine);
    }

    protected Scenario scenario;

    protected Scenario startByKey(ProcessDefinition processDefinition, String businessKey, Map<String, Object> processVars) {
        return Scenario.run(processScenario)
                .startBy(() -> camundaHelper.startProcessByProcessDefinition(processDefinition,
                        businessKey, processVars))
                .engine(processEngine)
                .execute();
    }

    protected Scenario startByMessage(Messages message, String businessKey, Map<String, Object> processVars) {
        return Scenario.run(processScenario)
                .startBy(() -> camundaHelper.startProcessByMessage(message, businessKey, processVars))
                .engine(processEngine)
                .execute();
    }

    protected Scenario startBeforeActivity(ProcessDefinition processDefinition, String activityId, String businessKey,
                                           Map<String, Object> processVars) {
        return Scenario.run(processScenario)
                .startBy(() -> runtimeService.createProcessInstanceByKey(processDefinition.getName())
                        .startBeforeActivity(activityId)
                        .setVariables(processVars)
                        .businessKey(businessKey)
                        .execute())
                .engine(processEngine)
                .execute();
    }

    protected Scenario startAfterActivity(ProcessDefinition processDefinition, String activityId, String businessKey,
                                          Map<String, Object> processVars) {
        return Scenario.run(processScenario)
                .startBy(() -> runtimeService.createProcessInstanceByKey(processDefinition.getName())
                        .startAfterActivity(activityId)
                        .setVariables(processVars)
                        .businessKey(businessKey)
                        .execute())
                .engine(processEngine)
                .execute();
    }

    protected Runner executeCallActivity() {
        return new ProcessRunnerImpl(null, processScenario);
    }

    protected Map<String, Object> createProcessVariables(SalesOrder salesOrder) {
        final String orderNumber = salesOrder.getOrderNumber();

        List<String> orderRowSkus;
        List<String> virtualOrderRowSkus = new ArrayList<>();
        String shippingType;
        String paymentType;
        String platformType;
        orderRowSkus = new ArrayList<>();
        final var order = (Order) salesOrder.getOriginalOrder();
        platformType = order.getOrderHeader().getPlatform().name();
        paymentType = getPaymentType(order.getOrderHeader().getPayments());
        shippingType = order.getOrderRows().get(0).getShippingType();
        for (OrderRows orderRow : order.getOrderRows()) {
            if (isShipped(orderRow.getShippingType())) {
                shippingType = orderRow.getShippingType();
                orderRowSkus.add(orderRow.getSku());
            } else {
                virtualOrderRowSkus.add(orderRow.getSku());
            }
        }


        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(SHIPMENT_METHOD.getName(), shippingType);
        processVariables.put(PLATFORM_TYPE.getName(), platformType);
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(PAYMENT_TYPE.getName(), paymentType);
        processVariables.put(ORDER_ROWS.getName(), orderRowSkus);
        processVariables.put(IS_ORDER_CANCELLED.getName(), false);
        processVariables.put(CUSTOMER_TYPE.getName(),
                salesOrder.isRecurringOrder() ? RECURRING.getType() : NEW.getType());
        processVariables.put(SALES_CHANNEL.getName(), salesOrder.getSalesChannel());

        if (!virtualOrderRowSkus.isEmpty()) {
            processVariables.put(VIRTUAL_ORDER_ROWS.getName(), virtualOrderRowSkus);
        }

        return processVariables;
    }

    protected String getPaymentType(List<Payments> payments) {
        return payments.stream()
                .map(Payments::getType)
                .filter(paymentType -> !VOUCHER.getName().equals(paymentType))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Order does not contain a valid payment type"));
    }

    protected boolean isShipped(String shippingType) {
        return !ShipmentMethod.NONE.getName().equals(shippingType);
    }

}
