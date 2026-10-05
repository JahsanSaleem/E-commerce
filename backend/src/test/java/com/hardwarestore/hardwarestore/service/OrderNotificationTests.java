package com.hardwarestore.hardwarestore.service;
import com.hardwarestore.hardwarestore.model.*;
import com.hardwarestore.hardwarestore.repository.*;
import com.hardwarestore.hardwarestore.dto.CheckoutRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test")
class OrderNotificationTests {
 @Autowired OrderService orders;@Autowired CartService carts;@Autowired UserRepository users;@Autowired ProductRepository products;@Autowired CategoryRepository categories;
 @MockitoBean VerificationMailer mail;
 @Test void sendsAfterCheckoutNotRetryAndCollectionCannotShip() {
  var u=new User();u.setName("Customer");u.setEmail(UUID.randomUUID()+"@example.com");u.setPassword("hash");u.setRole(Role.CUSTOMER);u=users.save(u);
  var c=new Category();c.setName("Notification test");c=categories.save(c);
  var p=products.save(new Product("Item","",BigDecimal.TEN,null,3,c));carts.addItem(u,p,1);
  String key=UUID.randomUUID().toString();var details=new CheckoutRequest("COLLECTION","Customer","0771234567",null);
  var order=orders.checkout(u,key,details);orders.checkout(u,key,details);
  verify(mail,timeout(2000).times(1)).sendOrderMessage(eq(u.getEmail()),contains("pending"),contains("Order received"));
  orders.updateOrderStatus(order.getOrderId(),OrderStatus.CONFIRMED);orders.updateOrderStatus(order.getOrderId(),OrderStatus.PROCESSING);
  assertThrows(IllegalArgumentException.class,()->orders.updateOrderStatus(order.getOrderId(),OrderStatus.SHIPPED));
  orders.updateOrderStatus(order.getOrderId(),OrderStatus.READY_FOR_COLLECTION);
  verify(mail,timeout(2000)).sendOrderMessage(eq(u.getEmail()),contains("ready for collection"),contains("Your order is ready"));
  orders.updateOrderStatus(order.getOrderId(),OrderStatus.DELIVERED);
 }
}
