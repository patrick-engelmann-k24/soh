package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNote;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNoteHeader;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderPackageItemLine;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.CurrencyType.convert;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getGrossValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getMultipliedValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.round;
import static java.time.LocalDateTime.now;

@Builder
@Value
@Jacksonized
public class SalesCreditNoteCreatedMessage {

    @JsonProperty("SalesCreditNote")
    SalesCreditNote salesCreditNote;

}
