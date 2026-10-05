package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.model.*;
import com.hardwarestore.hardwarestore.repository.*;
import com.hardwarestore.hardwarestore.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CheckoutIntegrityTests {
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItems;
    @Autowired CartItemRepository cartItems;
    @Autowired CartRepository cartRepository;
    User customer() {
        var user=new User();user.setName("Test");user.setEmail(UUID.randomUUID()+"@example.com");user.setPassword("test hash");user.setRole(Role.CUSTOMER);return users.save(user);
    }
    Product product(int quantity) {
        var category=new Category();category.setName("Test category");category=categories.save(category);
        return products.save(new Product("Test product","",new BigDecimal("12.50"),null,quantity,category));
    }
    @Test void checkoutCreatesItemsDeductsStockAndClearsCart() {
        var user=customer();var product=product(5);carts.addItem(user,product,2);
        var order=orders.checkout(user);
        assertEquals(new BigDecimal("25.00"),order.getTotalAmount());
        assertEquals(3,products.findById(product.getProductId()).orElseThrow().getQuantity());
        assertTrue(carts.getCartItems(user).isEmpty());assertEquals(1,orderItems.findByOrder(order).size());
    }
    @Test void failedMultiProductCheckoutRollsBackEarlierStockUpdates() {
        var user=customer();var first=product(3);var second=product(1);
        carts.addItem(user,first,2);carts.addItem(user,second,1);
        second.setQuantity(0);products.save(second);
        assertThrows(ResourceConflictException.class,()->orders.checkout(user));
        assertEquals(3,products.findById(first.getProductId()).orElseThrow().getQuantity());
        assertEquals(2,carts.getCartItems(user).size());assertTrue(orderRepository.findByCustomer(user).isEmpty());
    }
    @Test void concurrentCustomersCannotBuyTheSameLastUnit() throws Exception {
        var first=customer();var second=customer();var product=product(1);
        carts.addItem(first,product,1);carts.addItem(second,product,1);
        var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> a=()->{start.await();try{orders.checkout(first);return true;}catch(ResourceConflictException ex){return false;}};
            Callable<Boolean> b=()->{start.await();try{orders.checkout(second);return true;}catch(ResourceConflictException ex){return false;}};
            var one=executor.submit(a);var two=executor.submit(b);start.countDown();
            assertNotEquals(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS));
            assertEquals(0,products.findById(product.getProductId()).orElseThrow().getQuantity());
            assertEquals(1,orderRepository.findByCustomer(first).size()+orderRepository.findByCustomer(second).size());
        } finally {executor.shutdownNow();}
    }
    @Test void emptyCartAndInvalidQuantitiesAreRejected() {
        var user=customer();var product=product(3);carts.getOrCreateCart(user);
        assertThrows(IllegalArgumentException.class,()->orders.checkout(user));
        assertThrows(IllegalArgumentException.class,()->carts.addItem(user,product,0));
        assertThrows(IllegalArgumentException.class,()->carts.addItem(user,product,4));
        assertTrue(carts.getCartItems(user).isEmpty());
    }
    @Test void concurrentAddsMergeIntoOneLineWithCorrectTotal() throws Exception {
        var user=customer();var product=product(5);var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try {
            Callable<Void> action=()->{start.await();carts.addItem(user,products.findById(product.getProductId()).orElseThrow(),1);return null;};
            var first=executor.submit(action);var second=executor.submit(action);start.countDown();
            first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
            var items=carts.getCartItems(user);assertEquals(1,items.size());assertEquals(2,items.get(0).getQuantity());
            assertEquals(new BigDecimal("25.00"),cartRepository.findByCustomer(user).orElseThrow().getTotalAmount());
        } finally {executor.shutdownNow();}
    }
    @Test void repeatedCheckoutKeyReturnsSameOrder() {
        var user=customer();var product=product(4);carts.addItem(user,product,2);
        String key=UUID.randomUUID().toString();
        var first=orders.checkout(user,key);var retry=orders.checkout(user,key);
        assertEquals(first.getOrderId(),retry.getOrderId());
        assertEquals(1,orderRepository.findByCustomer(user).size());
        assertEquals(2,products.findById(product.getProductId()).orElseThrow().getQuantity());
    }
    @Test void concurrentDuplicateCheckoutReturnsOneOrder() throws Exception {
        var user=customer();var product=product(4);carts.addItem(user,product,2);
        String key=UUID.randomUUID().toString();var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try {
            Callable<Order> action=()->{start.await();return orders.checkout(user,key);};
            var first=executor.submit(action);var second=executor.submit(action);start.countDown();
            assertEquals(first.get(10,TimeUnit.SECONDS).getOrderId(),second.get(10,TimeUnit.SECONDS).getOrderId());
            assertEquals(1,orderRepository.findByCustomer(user).size());
            assertEquals(2,products.findById(product.getProductId()).orElseThrow().getQuantity());
        } finally {executor.shutdownNow();}
    }
    @Test void cancellationRestoresStockWithoutDoubleRestocking() {
        var user=customer();var product=product(2);carts.addItem(user,product,2);var order=orders.checkout(user);
        orders.updateOrderStatus(order.getOrderId(),OrderStatus.CANCELLED);
        orders.updateOrderStatus(order.getOrderId(),OrderStatus.CANCELLED);
        assertEquals(2,products.findById(product.getProductId()).orElseThrow().getQuantity());
    }
    @Test void deliveryFeeAndSnapshotAreServerCalculatedAndRetrySafe() {
        var user=customer();var product=product(5);carts.addItem(user,product,2);
        var details=new com.hardwarestore.hardwarestore.dto.CheckoutRequest("DELIVERY","Recipient","0771234567","123 Test Street, Colombo");
        String key=UUID.randomUUID().toString();var order=orders.checkout(user,key,details);
        assertEquals(new BigDecimal("274.00"),order.getTotalAmount());assertEquals(new BigDecimal("249.00"),order.getDeliveryFee());
        assertEquals(details.address(),order.getAddress());assertEquals(order.getOrderId(),orders.checkout(user,key,details).getOrderId());
    }
    @Test void collectionHasNoFeeAndInvalidAddressDoesNotDeductStock() {
        var user=customer();var product=product(2);carts.addItem(user,product,1);
        var invalid=new com.hardwarestore.hardwarestore.dto.CheckoutRequest("DELIVERY","Recipient","0771234567","");
        assertThrows(IllegalArgumentException.class,()->orders.checkout(user,UUID.randomUUID().toString(),invalid));
        assertEquals(2,products.findById(product.getProductId()).orElseThrow().getQuantity());
        var details=new com.hardwarestore.hardwarestore.dto.CheckoutRequest("COLLECTION","Recipient","0771234567",null);
        var order=orders.checkout(user,UUID.randomUUID().toString(),details);assertEquals(new BigDecimal("12.50"),order.getTotalAmount());assertEquals(BigDecimal.ZERO,order.getDeliveryFee());assertNull(order.getAddress());
    }
}
