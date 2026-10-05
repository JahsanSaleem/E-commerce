package com.hardwarestore.hardwarestore.repository;

import com.hardwarestore.hardwarestore.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product> {
    long countByQuantityLessThanEqual(int quantity);
    java.util.List<Product> findByQuantityLessThanEqualOrderByQuantityAscProductIdAsc(int quantity, org.springframework.data.domain.Pageable page);

    @Modifying
    @Query("update Product p set p.quantity = p.quantity - :quantity, p.version = p.version + 1 where p.productId = :id and p.quantity >= :quantity")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("update Product p set p.quantity = p.quantity + :quantity, p.version = p.version + 1 where p.productId = :id")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);

}