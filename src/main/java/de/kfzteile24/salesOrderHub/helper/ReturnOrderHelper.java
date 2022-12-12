package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNote;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNoteHeader;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderPackageItemLine;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.returnorder.ReturnOrderServiceAdaptor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.SalesOrderType.DROPSHIPMENT;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getMultipliedValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.getSumValue;
import static de.kfzteile24.salesOrderHub.helper.CalculationUtil.round;
import static de.kfzteile24.salesOrderHub.helper.OrderUtil.getOrderGroupIdFromOrderNumber;
import static java.time.LocalDateTime.now;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnOrderHelper {

    @NonNull
    private final ReturnOrderServiceAdaptor adaptor;

    @NonNull
    private final SalesOrderReturnService salesOrderReturnService;

    @NonNull
    private final SalesOrderService salesOrderService;

    public SalesCreditNoteCreatedMessage buildSalesCreditNoteCreatedMessage(DropshipmentPurchaseOrderReturnConfirmedMessage message) {
        var creditNoteNumber = salesOrderReturnService.createCreditNoteNumber();
        var orderNumber = message.getSalesOrderNumber();
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        return SalesCreditNoteCreatedMessage.builder()
                .salesCreditNote(buildSalesCreditNote(salesOrder, buildCreditNoteLines(message), creditNoteNumber))
                .build();
    }

    private List<CreditNoteLine> buildCreditNoteLines(DropshipmentPurchaseOrderReturnConfirmedMessage message) {
        var salesOrders = adaptor.getSalesOrderList(
                getOrderGroupIdFromOrderNumber(message.getSalesOrderNumber()),
                DROPSHIPMENT);

        var salesOrdersOrderRowsList = salesOrders.stream()
                .flatMap(order -> order.getLatestJson().getOrderRows().stream())
                .filter(row -> !row.getIsCancelled())
                .collect(Collectors.toList());
        var eventItemList = message.getPackages().stream()
                .flatMap(c -> c.getItems().stream())
                .collect(Collectors.toList());

        List<CreditNoteLine> creditNoteLines = new ArrayList<>();
        for (var orderRow : salesOrdersOrderRowsList) {
            for (var item : eventItemList) {
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
        checkIfAllItemsAreCovered(creditNoteLines, eventItemList, salesOrders.get(0).getOrderGroupId(), message.getExternalOrderNumber());
        return creditNoteLines;
    }

    private static SalesCreditNote buildSalesCreditNote(
            SalesOrder salesOrder,
            List<CreditNoteLine> creditNoteLines,
            String creditNoteNumber) {
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
        return SalesCreditNote.builder()
                .deliveryNotes(new ArrayList<>())
                .salesCreditNoteHeader(salesCreditNoteHeader)
                .build();
    }

    private static void checkIfAllItemsAreCovered(List<CreditNoteLine> creditNoteLines,
                                                  List<DropshipmentPurchaseOrderPackageItemLine> eventItemList,
                                                  String orderGroupId, String externalOrderNumber) {
        List<String> lineSkuList = creditNoteLines.stream()
                .map(CreditNoteLine::getItemNumber)
                .collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder();
        eventItemList.forEach(item -> {
            if (!lineSkuList.contains(item.getProductNumber())) {
                stringBuilder.append(", ").append(item.getProductNumber());
            }
        });
        if (stringBuilder.length() > 0) {
            throw new NotFoundException("The skus" + stringBuilder +
                    " are missing in received dropshipment purchase order return confirmed message with" +
                    " Sales Order Group Id: " + orderGroupId +
                    ", External Order Number: " + externalOrderNumber);
        }
    }
}
