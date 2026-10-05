package com.hardwarestore.hardwarestore.repository;

import com.hardwarestore.hardwarestore.model.Order;
import com.hardwarestore.hardwarestore.model.OrderStatus;
import com.hardwarestore.hardwarestore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);


    Optional<Order> findByCustomerAndCheckoutKey(User customer, String checkoutKey);

    List<Order> findByCustomer(User customer);

    List<Order> findByStatus(OrderStatus status);
    long countByStatus(OrderStatus status);
    @Query("select coalesce(sum(o.totalAmount),0) from Order o where o.status = :status")
    java.math.BigDecimal totalByStatus(@Param("status") OrderStatus status);
    @Query("select year(o.orderDate), month(o.orderDate), sum(o.totalAmount), count(o) from Order o where o.status = :status and o.orderDate >= :since group by year(o.orderDate), month(o.orderDate) order by year(o.orderDate), month(o.orderDate)")
    List<Object[]> monthlyDelivered(@Param("status") OrderStatus status, @Param("since") java.time.LocalDateTime since);

}