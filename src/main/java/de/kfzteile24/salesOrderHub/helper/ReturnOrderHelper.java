package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNote;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNoteHeader;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderPackageItemLine;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getMultipliedValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.round;
import static java.time.LocalDateTime.now;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnOrderHelper {

    public SalesCreditNoteCreatedMessage buildSalesCreditNoteCreatedMessage(
            DropshipmentPurchaseOrderReturnConfirmedMessage message, SalesOrder salesOrder, String creditNoteNumber) {
        List<CreditNoteLine> creditNoteLines = new ArrayList<>();
        for (OrderRows orderRow : salesOrder.getLatestJson().getOrderRows()) {
            for (DropshipmentPurchaseOrderPackageItemLine item : message.getPackages().stream()
                    .flatMap(c -> c.getItems().stream()).collect(Collectors.toList())) {
                if (StringUtils.pathEquals(item.getProductNumber(), orderRow.getSku())) {
                    var quantity = Optional.of(BigDecimal.valueOf(item.getQuantity()).negate()).orElse(BigDecimal.ZERO);
                    var unitNetAmount = Optional.ofNullable(orderRow.getUnitValues().getDiscountedNet()).orElse(BigDecimal.ZERO);
                    var lineNetAmount = round(getMultipliedValue(unitNetAmount, quantity));
                    var taxRate = Optional.ofNullable(orderRow.getTaxRate()).orElse(BigDecimal.ZERO);
                    var unitGrossAmount = Optional.ofNullable(orderRow.getUnitValues().getDiscountedGross()).orElse(BigDecimal.ZERO);
                    var lineGrossAmount = round(getMultipliedValue(unitGrossAmount, quantity));
                    var lineTaxAmount = lineGrossAmount.subtract(lineNetAmount);
                    CreditNoteLine creditNoteLine = CreditNoteLine.builder()
                            .itemNumber(orderRow.getSku())
                            .taxRate(taxRate)
                            .quantity(quantity)
                            .unitNetAmount(unitNetAmount)
                            .lineNetAmount(lineNetAmount)
                            .unitGrossAmount(unitGrossAmount)
                            .lineGrossAmount(lineGrossAmount)
                            .lineTaxAmount(lineTaxAmount)
                            .isShippingCost(false)
                            .build();
                    creditNoteLines.add(creditNoteLine);
                }
            }
        }
        var salesCreditNoteHeader = SalesCreditNoteHeader.builder()
                .creditNoteNumber(creditNoteNumber)
                .creditNoteDate(now())
                .currencyCode(salesOrder.getLatestJson().getOrderHeader().getOrderCurrency())
                .billingAddress(Address.fromBillingAddress(salesOrder.getLatestJson().getOrderHeader().getBillingAddress()))
                .creditNoteLines(creditNoteLines)
                .orderGroupId(salesOrder.getLatestJson().getOrderHeader().getOrderGroupId())
                .orderNumber(salesOrder.getLatestJson().getOrderHeader().getOrderNumber())
                .grossAmount(getSumValue(CreditNoteLine::getLineGrossAmount, creditNoteLines))
                .netAmount(getSumValue(CreditNoteLine::getLineNetAmount, creditNoteLines))
                .build();
        var salesCreditNote = SalesCreditNote.builder()
                .deliveryNotes(new ArrayList<>())
                .salesCreditNoteHeader(salesCreditNoteHeader)
                .build();
        return SalesCreditNoteCreatedMessage.builder()
                .salesCreditNote(salesCreditNote)
                .build();
    }
}
