package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.aggregator.Aggregators;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Totals;
import de.kfzteile24.soh.order.dto.UnitValues;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class SalesOrderReturnIntegrationTest {

    @Autowired
    private SalesOrderRowService salesOrderRowService;

    // may be later it's better using csv file source, much more readable and editable (with column names as well)
    @ParameterizedTest
    @CsvSource(value = {
              "2.20, 2,    0,    0, 2.20,   2,    1.10, 1,    0,   0, 1.10,   1,     0.00, 0.00, 0, 0, 0.00, 0.00,     2, -1.00, -1.10, -1.10,    2.20, 2,    0, 2.20, 0.20, 1.10,    1, 1.10,    0.00, 0.00, 0.00, 0.00, 0, 0, 0.00,  0.00,    0",
              "2.20, 2,    0,    0, 2.20,   2,    1.10, 1,    0,   0, 1.10,   1,     2.20, 2.00, 0, 0, 2.20, 2.00,     1, -1.00, -1.10, -1.10,    2.20, 2,    0, 2.20, 0.20, 1.10,    1,  0.2,    2.20, 2.00, 2.20, 2.00, 0, 0, 2.20,  0.00,    1",
              "2.20, 2,  0.2, 0.22, 1.98, 1.8,    1.10, 1, 0.11, 0.1, 0.99, 0.9,     2.20, 2.00, 0, 0, 2.20, 2.00,     1, -1.00, -1.10, -1.10,    2.20, 2, 1.98,  1.8, 0.22,  0.2, 1.98,  0.18,   2.20, 2.00, 2.20, 2.00, 0, 0, 2.20, -0.02,    1"
    })
    void testRecalculateOrderByReturnsOneRowAndOneReturnedItem(@AggregateWith(Aggregators.SumValuesAggregator.class) SumValues sumValues,
                                                               @AggregateWith(Aggregators.UnitValuesAggregator.class) UnitValues unitValues,
                                                               @AggregateWith(Aggregators.UpdatedSumValuesAggregator.class) SumValues updatedSumValues,
                                                               @AggregateWith(Aggregators.CreditNoteLineAggregator.class) CreditNoteLine item,
                                                               @AggregateWith(Aggregators.TotalsAggregator.class) Totals totals,
                                                               @AggregateWith(Aggregators.UpdatedTotalsAggregator.class) Totals updatedTotals,
                                                               ArgumentsAccessor arguments) {

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        var orderRows = List.of(
                createOrderRow("sku-1", sumValues, unitValues, 2)
        );

        salesOrder.getLatestJson().getOrderHeader().setTotals(totals);
        salesOrder.getLatestJson().setOrderRows(orderRows);

        var returnLatestJson = salesOrderRowService.recalculateOrderByReturns(salesOrder, List.of(item));
        var returnOrderRow = returnLatestJson.getOrderRows().get(0);

        assertReturnOrderValues(returnLatestJson, returnOrderRow, updatedSumValues, updatedTotals, arguments.get(38, BigDecimal.class));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "2.20, 2, 0, 0, 2.20, 2,    1.10, 1, 0, 0, 1.10, 1,    0.00, 0.00, 0, 0, 0.00, 0.00,   2, -1.00, -1.10, -1.10,   4.40, 4.00, 0, 2.40, 0.40, 2.20, 2, 2.20,   0.00, 0.00, 0.00, 0.00, 0, 0, 0.00, 0.00,  0",
            "2.20, 2, 0, 0, 2.20, 2,    1.10, 1, 0, 0, 1.10, 1,    2.20, 2.00, 0, 0, 2.20, 2.00,   1, -1.00, -1.10, -1.10,   4.40, 4.00, 0, 2.40, 0.40, 2.20, 3,  0.4,   4.40, 2.00, 4.40, 2.00, 0, 0, 4.40, 0.00,  1"
    })
    void testRecalculateOrderByReturnsTwoRowsAndTwoReturnedItems(@AggregateWith(Aggregators.SumValuesAggregator.class) SumValues sumValues,
                                                                 @AggregateWith(Aggregators.UnitValuesAggregator.class) UnitValues unitValues,
                                                                 @AggregateWith(Aggregators.UpdatedSumValuesAggregator.class) SumValues updatedSumValues,
                                                                 @AggregateWith(Aggregators.CreditNoteLineAggregator.class) CreditNoteLine item,
                                                                 @AggregateWith(Aggregators.AnotherCreditNoteLineAggregator.class) CreditNoteLine anotherItem,
                                                                 @AggregateWith(Aggregators.TotalsAggregator.class) Totals totals,
                                                                 @AggregateWith(Aggregators.UpdatedTotalsAggregator.class) Totals updatedTotals,
                                                                 ArgumentsAccessor arguments) {

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        var orderRows = List.of(
                createOrderRow("sku-1", sumValues, unitValues, 2),
                createOrderRow("sku-3", sumValues, unitValues, 2)
        );

        salesOrder.getLatestJson().getOrderHeader().setTotals(totals);
        salesOrder.getLatestJson().setOrderRows(orderRows);

        var returnLatestJson = salesOrderRowService.recalculateOrderByReturns(salesOrder, List.of(item, anotherItem));
        returnLatestJson.getOrderRows().forEach(returnOrderRow ->
                        assertReturnOrderValues(returnLatestJson, returnOrderRow, updatedSumValues, updatedTotals, arguments.get(38, BigDecimal.class))
                );
    }

    private static void assertReturnOrderValues(Order returnLatestJson, OrderRows returnOrderRow,
                                         SumValues updatedSumValues, Totals updatedTotals, BigDecimal quantity) {
        assertThat(returnOrderRow.getQuantity()).isEqualTo(quantity);

        assertThat(returnLatestJson.getOrderHeader().getTotals())
                .isEqualToComparingOnlyGivenFields(updatedTotals, "goodsTotalGross",
                "goodsTotalNet", "grandTotalGross", "grandTotalNet", "totalDiscountGross", "totalDiscountNet", "paymentTotal", "grandTotalTaxes");

        assertThat(returnOrderRow.getSumValues())
                .isEqualToComparingOnlyGivenFields(updatedSumValues, "goodsValueGross",
                        "goodsValueNet", "discountGross", "discountNet", "totalDiscountedGross", "totalDiscountedNet");
    }

    private static OrderRows createOrderRow(String sku, SumValues sumValues, UnitValues unitValues, Integer quantity) {
        return OrderRows.builder()
                .sku(sku)
                .sumValues(sumValues)
                .unitValues(unitValues)
                .taxRate(BigDecimal.valueOf(10))
                .quantity(BigDecimal.valueOf(quantity))
                .build();
    }
}
