package de.kfzteile24.salesOrderHub.utils;

import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrderUtilTest {

    @InjectMocks
    private OrderUtil orderUtil;

    @Test
    void testGetLastRowKey() {
        final var salesOrder = getSalesOrder(readResource("examples/ecpOrderMessageWithTwoRows.json"));

        final var lastRowKey = orderUtil.getLastRowKey(salesOrder);

        assertThat(lastRowKey).isEqualTo(2);
    }

    @Test
    void testUpdateLastRowKey() {
        final var salesOrder = getSalesOrder(readResource("examples/ecpOrderMessageWithTwoRows.json"));
        var lastRowKey = 2;

        lastRowKey = orderUtil.updateLastRowKey(salesOrder, "2270-13012", lastRowKey);

        assertThat(lastRowKey).isEqualTo(2);

        lastRowKey = orderUtil.updateLastRowKey(salesOrder, "ABC-1", lastRowKey);

        assertThat(lastRowKey).isEqualTo(3);

        lastRowKey = orderUtil.updateLastRowKey(salesOrder, "2270-13013", lastRowKey);

        assertThat(lastRowKey).isEqualTo(3);

        lastRowKey = orderUtil.updateLastRowKey(salesOrder, "ABC-2", lastRowKey);

        assertThat(lastRowKey).isEqualTo(4);
    }
}
