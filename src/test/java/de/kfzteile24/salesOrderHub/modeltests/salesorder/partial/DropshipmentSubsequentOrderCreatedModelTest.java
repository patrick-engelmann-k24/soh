package de.kfzteile24.salesOrderHub.modeltests.salesorder.partial;

import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.mockito.Mockito.verify;

@DisplayName("Dropshipment Subsequent Order Created model test")
@Slf4j(topic = "Dropshipment Subsequent Order Created model test")
class DropshipmentSubsequentOrderCreatedModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("Dropshipment Subsequent Order Created"))
    @DisplayName("Start process after msgDropshipmentSubsequentOrderCreated")
    void testDropshipmentSubsequentOrderCreated(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        scenario = startByMessage(DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED,
                businessKey, processVariables);

        verify(processScenario).hasCompleted("eventThrowMsgSubsequentDropshipmentOrderCreated");
        verify(processScenario).hasCompleted("Gateway_0uq8npi");
        verify(processScenario).hasCompleted("eventEndMsgOrderCompleted");
    }
}
