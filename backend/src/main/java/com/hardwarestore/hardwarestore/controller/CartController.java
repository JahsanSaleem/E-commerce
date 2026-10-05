package com.hardwarestore.hardwarestore.controller;

import com.hardwarestore.hardwarestore.dto.CartItemResponse;
import com.hardwarestore.hardwarestore.dto.CartResponse;
import com.hardwarestore.hardwarestore.dto.OrderResponse;
import com.hardwarestore.hardwarestore.exception.ResourceNotFoundException;
import com.hardwarestore.hardwarestore.model.Cart;
import com.hardwarestore.hardwarestore.model.CartItem;
import com.hardwarestore.hardwarestore.model.Order;
import com.hardwarestore.hardwarestore.model.Product;
import com.hardwarestore.hardwarestore.model.User;
import com.hardwarestore.hardwarestore.repository.ProductRepository;
import com.hardwarestore.hardwarestore.repository.UserRepository;
import com.hardwarestore.hardwarestore.service.CartService;
import com.hardwarestore.hardwarestore.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartController(CartService cartService,
                          OrderService orderService,
                          UserRepository userRepository,
                          ProductRepository productRepository) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/cart/{userId}")
    public CartResponse getCart(@PathVariable Long userId, HttpSession session) {

        User customer = getAuthenticatedCustomer(userId, session);

        Cart cart = cartService.getOrCreateCart(customer);

        List<CartItemResponse> items = cartService.getCartItems(customer)
                .stream()
                .map(this::toCartItemResponse)
                .toList();

        return new CartResponse(
                cart.getCartId(),
                customer.getId(),
                cart.getTotalAmount(),
                items
        );
    }

    @PostMapping("/cart/{userId}/items/{productId}")
    public CartItemResponse addItem(@PathVariable Long userId,
                                    @PathVariable Long productId,
                                    @RequestParam Integer quantity,
                                    HttpSession session) {

        User customer = getAuthenticatedCustomer(userId, session);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + productId));

        CartItem cartItem = cartService.addItem(
                customer,
                product,
                quantity
        );

        return toCartItemResponse(cartItem);
    }

    @PutMapping("/cart/{userId}/items/{productId}")
    public CartItemResponse updateQuantity(@PathVariable Long userId,
                                           @PathVariable Long productId,
                                           @RequestParam Integer quantity,
                                           HttpSession session) {

        User customer = getAuthenticatedCustomer(userId, session);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + productId));

        CartItem cartItem = cartService.updateQuantity(
                customer,
                product,
                quantity
        );

        return toCartItemResponse(cartItem);
    }

    @DeleteMapping("/cart/{userId}/items/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long userId,
                                           @PathVariable Long productId,
                                           HttpSession session) {

        User customer = getAuthenticatedCustomer(userId, session);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + productId));

        cartService.removeItem(customer, product);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/checkout/options")
    public java.util.Map<String, Object> options() {
        return java.util.Map.of("deliveryFee", orderService.deliveryFee(), "collectionFee", java.math.BigDecimal.ZERO);
    }

    @PostMapping("/cart/{userId}/checkout")
    public OrderResponse checkout(@PathVariable Long userId, HttpSession session,
                                  @RequestHeader(value = "X-Checkout-Key", required = false) String checkoutKey,
                                  @jakarta.validation.Valid @RequestBody com.hardwarestore.hardwarestore.dto.CheckoutRequest details) {

        User customer = getAuthenticatedCustomer(userId, session);

        details.validate();
        Order order = orderService.checkout(customer, checkoutKey == null ? java.util.UUID.randomUUID().toString() : checkoutKey, details);

        return OrderResponse.from(order);
    }

    private CartItemResponse toCartItemResponse(CartItem cartItem) {

        return new CartItemResponse(
                cartItem.getCartItemId(),
                cartItem.getProduct().getProductId(),
                cartItem.getProduct().getName(),
                cartItem.getQuantity(),
                cartItem.getUnitPrice()
        );
    }

    private User getAuthenticatedCustomer(Long requestedUserId, HttpSession session) {
        Long authenticatedUserId = (Long) session.getAttribute("userId");

        if (authenticatedUserId == null || !authenticatedUserId.equals(requestedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please login to access this cart"
            );
        }

        return userRepository.findById(authenticatedUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + authenticatedUserId
                        )
                );
    }
}
