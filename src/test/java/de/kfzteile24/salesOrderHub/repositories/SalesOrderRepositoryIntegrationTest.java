package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SalesOrderRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SalesOrderRepository repository;
    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Test
    void testFindOrderNumberByOrderGroupId() {
        var orderGroupId = RandomStringUtils.randomNumeric(9);
        createSalesOrder(orderGroupId, orderGroupId);
        var orderNumber1 = orderGroupId + "-1";
        createSalesOrder(orderNumber1, orderGroupId);
        var orderNumber2 = orderGroupId + "-2";
        createSalesOrder(orderNumber2, orderGroupId);

        var orderNumbers = repository.findOrderNumberByOrderGroupId(orderGroupId);

        assertThat(orderNumbers).isSubsetOf(orderGroupId, orderNumber1, orderNumber2);
    }

    private void createSalesOrder(String orderNumber, String orderGroupId) {
        salesOrderUtil.createPersistedSalesOrder(
                orderNumber, orderGroupId, LocalDateTime.now(), "sku1");
    }

    @BeforeEach
    public void prepare() {
        repository.deleteAll();
    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        repository.deleteAll();
    }
}