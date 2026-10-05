package com.hardwarestore.hardwarestore.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(unique = true, length = 36)
    private String checkoutKey;

    public void setCheckoutKey(String checkoutKey) { this.checkoutKey = checkoutKey; }

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User customer;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(length=20) private String fulfilment;
    private String recipientName;
    @Column(length=25) private String phone;
    @Column(length=500) private String address;
    @Column(precision=10, scale=2) private BigDecimal deliveryFee;
    public String getFulfilment() { return fulfilment; }
    public void setFulfilment(String value) { fulfilment=value; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String value) { recipientName=value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone=value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { address=value; }
    public BigDecimal getDeliveryFee() { return deliveryFee == null ? BigDecimal.ZERO : deliveryFee; }
    public void setDeliveryFee(BigDecimal value) { deliveryFee=value; }

    public Order() {
    }

    public Order(User customer,
                 LocalDateTime orderDate,
                 BigDecimal totalAmount,
                 OrderStatus status) {
        this.customer = customer;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
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