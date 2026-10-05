package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.model.Cart;
import com.hardwarestore.hardwarestore.model.CartItem;
import com.hardwarestore.hardwarestore.model.Order;
import com.hardwarestore.hardwarestore.model.OrderItem;
import com.hardwarestore.hardwarestore.model.OrderStatus;
import com.hardwarestore.hardwarestore.model.Product;
import com.hardwarestore.hardwarestore.model.User;
import com.hardwarestore.hardwarestore.exception.ResourceNotFoundException;
import com.hardwarestore.hardwarestore.exception.ResourceConflictException;
import com.hardwarestore.hardwarestore.repository.CartItemRepository;
import com.hardwarestore.hardwarestore.repository.CartRepository;
import com.hardwarestore.hardwarestore.repository.OrderItemRepository;
import com.hardwarestore.hardwarestore.repository.OrderRepository;
import com.hardwarestore.hardwarestore.repository.ProductRepository;
import com.hardwarestore.hardwarestore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final PasswordResetMailDispatcher notifications;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository, PasswordResetMailDispatcher notifications
    ) {
        this.notifications=notifications;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order checkout(User customer) {
        return checkout(customer, java.util.UUID.randomUUID().toString());
    }

    @org.springframework.beans.factory.annotation.Value("${store.delivery-fee:249.00}")
    private BigDecimal configuredDeliveryFee;
    public BigDecimal deliveryFee() {
        if (configuredDeliveryFee.signum()<0) throw new IllegalStateException("Delivery fee cannot be negative");
        return configuredDeliveryFee.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }
    @Transactional
    public Order checkout(User customer, String checkoutKey) {
        return checkout(customer, checkoutKey, null);
    }
    @Transactional
    public Order checkout(User customer, String checkoutKey, com.hardwarestore.hardwarestore.dto.CheckoutRequest details) {
        if (details != null) details.validate();
        try { java.util.UUID.fromString(checkoutKey); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Checkout key must be a UUID"); }
        userRepository.findByIdForUpdate(customer.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var previous = orderRepository.findByCustomerAndCheckoutKey(customer, checkoutKey);
        if (previous.isPresent()) return previous.get();

        Cart cart = cartRepository.findByCustomer(customer)
                .orElseThrow(() ->
                        new IllegalArgumentException("Cart not found")
                );

        List<CartItem> cartItems =
                cartItemRepository.findByCart(cart);

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot checkout an empty cart"
            );
        }

        // Validate cart item quantity and stored unit price
        for (CartItem cartItem : cartItems) {

            if (cartItem.getQuantity() == null
                    || cartItem.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Cart item quantity must be greater than zero"
                );
            }

            if (cartItem.getUnitPrice() == null
                    || cartItem.getUnitPrice()
                    .compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Cart item price cannot be negative"
                );
            }
        }

        // Conditional updates are evaluated by the database, preventing overselling.
        // Stable product order reduces deadlocks for overlapping multi-product carts.
        cartItems = cartItems.stream().sorted(java.util.Comparator.comparing(item -> item.getProduct().getProductId())).toList();
        for (CartItem item : cartItems) {
            if (productRepository.deductStock(item.getProduct().getProductId(), item.getQuantity()) != 1) {
                throw new ResourceConflictException("Not enough stock for product: " + item.getProduct().getName() + ". Refresh your cart.", null);
            }
        }

        // Calculate total order amount
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {

            BigDecimal itemTotal =
                    cartItem.getUnitPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            cartItem.getQuantity()
                                    )
                            );

            totalAmount = totalAmount.add(itemTotal);
        }

        BigDecimal fee = details != null && details.fulfilment().equals("DELIVERY") ? deliveryFee() : BigDecimal.ZERO;
        // Create order
        Order order = new Order(
                customer,
                LocalDateTime.now(),
                totalAmount.add(fee),
                OrderStatus.PENDING
        );

        if (details != null) {
            order.setFulfilment(details.fulfilment()); order.setRecipientName(details.recipientName().trim());
            order.setPhone(details.phone().trim()); order.setAddress(details.fulfilment().equals("DELIVERY") ? details.address().trim() : null);
        }
        order.setDeliveryFee(fee);
        order.setCheckoutKey(checkoutKey);
        order = orderRepository.save(order);

        // Create order items and reduce inventory stock
        for (CartItem cartItem : cartItems) {

            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    cartItem.getQuantity(),
                    cartItem.getUnitPrice()
            );

            orderItemRepository.save(orderItem);

        }

        // Clear cart after successful checkout
        cartItemRepository.deleteAll(cartItems);

        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);

        notifyCustomer(order, "Order received");
        return order;
    }

    public List<Order> getCustomerOrders(User customer) {
        return orderRepository.findByCustomer(customer);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public List<OrderItem> getOrderItems(Order order) {
        return orderItemRepository.findByOrder(order);
    }

    @Transactional
    public Order updateOrderStatus(
            Long orderId,
            OrderStatus status
    ) {

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: " + orderId
                        )
                );

        if (order.getStatus() == status) return order;
        boolean collection = "COLLECTION".equals(order.getFulfilment());
        if ((collection && status == OrderStatus.SHIPPED) || (!collection && status == OrderStatus.READY_FOR_COLLECTION))
            throw new IllegalArgumentException("This status does not match the order fulfilment method.");
        if (!order.getStatus().nextStatuses().contains(status)) {
            throw new ResourceConflictException("Cannot change an order from " + order.getStatus() + " to " + status + ".", null);
        }
        if (status == OrderStatus.CANCELLED) {
            // Keep consistent product lock order when cancelling multi-product orders.
            var items = orderItemRepository.findByOrder(order).stream()
                    .sorted(java.util.Comparator.comparing(item -> item.getProduct().getProductId())).toList();
            for (OrderItem item : items) {
                if (productRepository.restoreStock(item.getProduct().getProductId(), item.getQuantity()) != 1) {
                    throw new ResourceConflictException("Unable to restore product stock. Order was not cancelled.", null);
                }
            }
        }
        order.setStatus(status);

        order=orderRepository.save(order);
        notifyCustomer(order, "Order status updated");
        return order;
    }
    private void notifyCustomer(Order order,String heading) {
        String method=order.getFulfilment()==null?"Not recorded":order.getFulfilment();
        String subject="Mustafa Hardware — order #"+order.getOrderId()+" "+order.getStatus().name().replace('_',' ').toLowerCase(java.util.Locale.ROOT);
        String body=heading+"\nOrder #"+order.getOrderId()+"\nStatus: "+order.getStatus().name().replace('_',' ')
            +"\nFulfilment: "+method+"\nDelivery fee: Rs. "+order.getDeliveryFee()+"\nTotal: Rs. "+order.getTotalAmount()
            +(order.getStatus()==OrderStatus.READY_FOR_COLLECTION?"\nYour order is ready. Contact the store to arrange collection.":"")
            +"\nSign in to Mustafa Hardware to view your order items. No online payment is recorded by this message.";
        notifications.sendOrder(order.getCustomer().getEmail(),subject,body);
    }
}
