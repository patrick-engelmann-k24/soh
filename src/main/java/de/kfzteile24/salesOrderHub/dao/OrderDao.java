package de.kfzteile24.salesOrderHub.dao;

import de.kfzteile24.salesOrderHub.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderDao extends JpaRepository<Order, Integer> {

    @Query("SELECT a FROM Order a WHERE a.id = :orderId")
    List<Order> getOrderList(@Param("orderId") Integer orderId);
}
