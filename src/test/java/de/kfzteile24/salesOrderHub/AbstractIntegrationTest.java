package de.kfzteile24.salesOrderHub;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Insights;
import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.PublishDropshipmentOrderCreatedDelegate;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import de.kfzteile24.salesOrderHub.services.salesorder.SalesOrderSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.ItemSplitService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperResolver;
import de.kfzteile24.salesOrderHub.services.sqs.PayloadResolverDecorator;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.spring.boot.starter.CamundaBpmConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Objects;

import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.reset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Import(CamundaBpmConfiguration.class)
public abstract class AbstractIntegrationTest implements ApplicationContextAware {

    protected static ProcessEngine processEngine;
    protected static ApplicationContext applicationContext;

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected MockMvc mvc;

    @MockBean
    protected Insights insights;

    @SpyBean
    protected AmazonSQSAsync amazonSQSAsync;
    @SpyBean
    protected ItemSplitService itemSplitService;
    @SpyBean
    protected SalesOrderService salesOrderService;
    @SpyBean
    protected SnsPublishService snsPublishService;
    @SpyBean
    protected KeyValuePropertyService keyValuePropertyService;
    @SpyBean
    protected DropshipmentOrderService dropshipmentOrderService;
    @SpyBean
    protected SleuthHelper sleuthHelper;
    @SpyBean
    protected CamundaHelper camundaHelper;
    @SpyBean
    protected SalesOrderRowService salesOrderRowService;
    @SpyBean
    protected SalesOrderReturnService salesOrderReturnService;
    @SpyBean
    protected PublishDropshipmentOrderCreatedDelegate publishDropshipmentOrderCreatedDelegate;
    @SpyBean
    protected NotificationMessagingTemplate notificationMessagingTemplate;
    @SpyBean
    protected MessageWrapperResolver messageWrapperResolver;
    @SpyBean
    protected PayloadResolverDecorator payloadResolverDecorator;
    @SpyBean
    protected SQSNamesConfig sqsNamesConfig;
    @SpyBean
    protected SalesOrderProcessService salesOrderCreateService;
    @SpyBean
    protected SalesOrderSqsReceiveService salesOrderSqsReceiveService;
    @SpyBean
    protected OrderUtil orderUtil;

    /**
     * The application context gets created only once for all the model tests
     */
    @Override
    @SneakyThrows
    public void setApplicationContext(@NonNull ApplicationContext newApplicationContext) {
        if (Objects.isNull(applicationContext)) {
            applicationContext = newApplicationContext;
            log.info(" ========== Application context created ==========");
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
            log.info(" ========== Default process engine created ==========");
        }
        log.info("Process engine name: {}. Object id: {}", processEngine.getName(), processEngine);
        reset();
        init(processEngine);
        doNothing().when(notificationMessagingTemplate).sendNotification(any(), any(), any());
    }
}
