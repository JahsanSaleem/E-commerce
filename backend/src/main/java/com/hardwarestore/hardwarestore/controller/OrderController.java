package com.hardwarestore.hardwarestore.controller;

import com.hardwarestore.hardwarestore.dto.OrderItemResponse;
import com.hardwarestore.hardwarestore.dto.OrderResponse;
import com.hardwarestore.hardwarestore.exception.ResourceNotFoundException;
import com.hardwarestore.hardwarestore.model.Order;
import com.hardwarestore.hardwarestore.model.OrderItem;
import com.hardwarestore.hardwarestore.model.OrderStatus;
import com.hardwarestore.hardwarestore.model.Role;
import com.hardwarestore.hardwarestore.model.User;
import com.hardwarestore.hardwarestore.repository.OrderRepository;
import com.hardwarestore.hardwarestore.repository.UserRepository;
import com.hardwarestore.hardwarestore.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService,
                           UserRepository userRepository,
                           OrderRepository orderRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/customer/{userId}")
    public List<OrderResponse> getCustomerOrders(
            @PathVariable Long userId,
            HttpSession session
    ) {

        requireSameUserOrAdmin(userId, session);

        User customer = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));

        return orderService.getCustomerOrders(customer)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @GetMapping("/status/{status}")
    public List<OrderResponse> getOrdersByStatus(
            @PathVariable OrderStatus status,
            HttpSession session
    ) {

        requireAdmin(session);

        return orderService.getOrdersByStatus(status)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @GetMapping("/{orderId}/items")
    public List<OrderItemResponse> getOrderItems(
            @PathVariable Long orderId,
            HttpSession session
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + orderId));

        requireSameUserOrAdmin(order.getCustomer().getId(), session);

        return orderService.getOrderItems(order)
                .stream()
                .map(this::toOrderItemResponse)
                .toList();
    }

    @PutMapping("/{orderId}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            HttpSession session
    ) {
        requireAdmin(session);

        return toOrderResponse(
                orderService.updateOrderStatus(orderId, status)
        );
    }

    private OrderResponse toOrderResponse(Order order) {

        return OrderResponse.from(order);
    }

    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {

        return new OrderItemResponse(
                orderItem.getOrderItemId(),
                orderItem.getProduct().getProductId(),
                orderItem.getProduct().getName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice()
        );
    }

    private void requireSameUserOrAdmin(Long requestedUserId, HttpSession session) {
        Long authenticatedUserId = (Long) session.getAttribute("userId");
        Role role = (Role) session.getAttribute("role");

        if (authenticatedUserId == null
                || (!authenticatedUserId.equals(requestedUserId) && role != Role.ADMIN && role != Role.STAFF)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "You cannot access another customer's orders"
            );
        }
    }

    private void requireAdmin(HttpSession session) {
        if (session.getAttribute("role") != Role.ADMIN && session.getAttribute("role") != Role.STAFF) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Staff or admin access required"
            );
        }
    }
}
