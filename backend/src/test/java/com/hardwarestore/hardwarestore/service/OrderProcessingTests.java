package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.model.*;
import com.hardwarestore.hardwarestore.repository.*;
import com.hardwarestore.hardwarestore.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderProcessingTests {
    OrderRepository orders=mock(OrderRepository.class);
    OrderItemRepository items=mock(OrderItemRepository.class);
    ProductRepository products=mock(ProductRepository.class);
    OrderService service=new OrderService(mock(CartRepository.class),mock(CartItemRepository.class),orders,items,products,mock(UserRepository.class),mock(PasswordResetMailDispatcher.class));
    private Order order(OrderStatus status) {
        var order=new Order(new User(),LocalDateTime.now(),BigDecimal.TEN,status);
        when(orders.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(invocation->invocation.getArgument(0));return order;
    }
    @ParameterizedTest @CsvSource({"PENDING,CONFIRMED","CONFIRMED,PROCESSING","PROCESSING,SHIPPED","SHIPPED,DELIVERED"})
    void forwardTransitionsSucceed(OrderStatus from,OrderStatus to) {
        var order=order(from);service.updateOrderStatus(1L,to);assertEquals(to,order.getStatus());verifyNoInteractions(products);
    }
    @ParameterizedTest @CsvSource({"PENDING,SHIPPED","CONFIRMED,PENDING","SHIPPED,CANCELLED","DELIVERED,CANCELLED","CANCELLED,CONFIRMED"})
    void invalidTransitionsDoNotChangeState(OrderStatus from,OrderStatus to) {
        var order=order(from);assertThrows(ResourceConflictException.class,()->service.updateOrderStatus(1L,to));
        assertEquals(from,order.getStatus());verify(orders,never()).save(any());verifyNoInteractions(products);
    }
    @Test void cancellationRestoresStockExactlyOnce() {
        var order=order(OrderStatus.PROCESSING);var product=new Product();product.setProductId(2L);
        when(items.findByOrder(order)).thenReturn(List.of(new OrderItem(order,product,3,BigDecimal.TEN)));
        when(products.restoreStock(2L,3)).thenReturn(1);
        service.updateOrderStatus(1L,OrderStatus.CANCELLED);service.updateOrderStatus(1L,OrderStatus.CANCELLED);
        verify(products,times(1)).restoreStock(2L,3);assertEquals(OrderStatus.CANCELLED,order.getStatus());
    }
}
