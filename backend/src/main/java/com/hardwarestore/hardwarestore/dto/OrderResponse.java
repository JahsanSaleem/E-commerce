package com.hardwarestore.hardwarestore.dto;

import com.hardwarestore.hardwarestore.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderResponse {

    private Long orderId;
    private Long customerId;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private OrderStatus status;

    public OrderResponse() {
    }

    public OrderResponse(Long orderId,
                         Long customerId,
                         LocalDateTime orderDate,
                         BigDecimal totalAmount,
                         OrderStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public static OrderResponse from(com.hardwarestore.hardwarestore.model.Order order) {
        var response=new OrderResponse(order.getOrderId(),order.getCustomer().getId(),order.getOrderDate(),order.getTotalAmount(),order.getStatus());
        response.fulfilment=order.getFulfilment(); response.recipientName=order.getRecipientName();
        response.phone=order.getPhone(); response.address=order.getAddress(); response.deliveryFee=order.getDeliveryFee();
        return response;
    }
    private String fulfilment, recipientName, phone, address;
    private BigDecimal deliveryFee;
    public String getFulfilment() { return fulfilment; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}