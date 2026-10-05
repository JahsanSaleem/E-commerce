package com.hardwarestore.hardwarestore.controller;
import com.hardwarestore.hardwarestore.repository.*;
import com.hardwarestore.hardwarestore.model.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin/dashboard")
public class DashboardController {
 private final ProductRepository products;private final UserRepository users;private final OrderRepository orders;
 public DashboardController(ProductRepository products,UserRepository users,OrderRepository orders){this.products=products;this.users=users;this.orders=orders;}
 @GetMapping public Map<String,Object> dashboard() {
  var counts=new LinkedHashMap<String,Long>();for(var status:OrderStatus.values())counts.put(status.name(),orders.countByStatus(status));
  var monthly=orders.monthlyDelivered(OrderStatus.DELIVERED,java.time.LocalDateTime.now().minusMonths(11).withDayOfMonth(1).toLocalDate().atStartOfDay()).stream()
   .map(row->Map.of("month",String.format("%04d-%02d",((Number)row[0]).intValue(),((Number)row[1]).intValue()),"total",row[2],"orders",row[3])).toList();
  return Map.of("products",products.count(),"users",users.count(),"orders",orders.count(),"statusCounts",counts,
   "deliveredOrderValue",orders.totalByStatus(OrderStatus.DELIVERED),"monthlyDelivered",monthly,
   "lowStockCount",products.countByQuantityLessThanEqual(5),"lowStock",products.findByQuantityLessThanEqualOrderByQuantityAscProductIdAsc(5,org.springframework.data.domain.PageRequest.of(0,10)));
 }
}
