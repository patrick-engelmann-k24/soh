package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;

import static de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig.OBJECT_MAPPER_WITH_BEAN_VALIDATION;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreSalesCreditNoteCreatedService {

    private ObjectMapper objectMapper;
    private final SalesOrderRowService salesOrderRowService;
    private final FeatureFlagConfig featureFlagConfig;

    @SneakyThrows
    @EnrichMessageForDlq
    public void handleCoreSalesCreditNoteCreated(String rawMessage, Integer receiveCount, String queueName) {

        if (featureFlagConfig.getIgnoreCoreCreditNote()) {
            log.info("Core Credit Note is ignored");
        } else {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage =
                    objectMapper.readValue(body, SalesCreditNoteCreatedMessage.class);

            var orderNumber = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
            log.info("Received core sales credit note created message with order number: {}", orderNumber);
            salesOrderRowService.handleSalesOrderReturn(salesCreditNoteCreatedMessage, RETURN_ORDER_CREATED, CORE_CREDIT_NOTE_CREATED);
        }
    }

        @Autowired
        public void setObjectMapper(@Qualifier(OBJECT_MAPPER_WITH_BEAN_VALIDATION) ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }
}
