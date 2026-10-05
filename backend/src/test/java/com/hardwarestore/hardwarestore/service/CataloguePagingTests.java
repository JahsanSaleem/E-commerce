package com.hardwarestore.hardwarestore.service;
import com.hardwarestore.hardwarestore.model.*;
import com.hardwarestore.hardwarestore.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test")
class CataloguePagingTests {
 @Autowired ProductService products; @Autowired ProductRepository repo; @Autowired CategoryRepository categories;
 @Test void pagesFilterSortAndClamp() {
  var c=new Category(); c.setName("Paging "+UUID.randomUUID()); c=categories.save(c);
  for(int i=1;i<=12;i++) {var p=new Product(); p.setName("Paging item "+i); p.setDescription("Test description"); p.setPrice(BigDecimal.valueOf(i)); p.setQuantity(i%2); p.setCategory(c); repo.save(p);}
  var first=products.browse(1,9,"",c.getCategoryId(),null,null,"","price-low");
  assertEquals(12L,first.get("totalElements")); assertEquals(9,((List<?>)first.get("content")).size());
  var last=products.browse(999,9,"",c.getCategoryId(),null,null,"","price-low"); assertEquals(2,last.get("page")); assertEquals(3,((List<?>)last.get("content")).size());
  var stock=products.browse(1,9,"test description",c.getCategoryId(),BigDecimal.valueOf(3),BigDecimal.valueOf(7),"in","price-high");
  assertEquals(3L,stock.get("totalElements")); assertEquals("Paging item 7",((Product)((List<?>)stock.get("content")).get(0)).getName());
  assertThrows(IllegalArgumentException.class,()->products.browse(1,101,"",null,null,null,"","name"));
 }
}
