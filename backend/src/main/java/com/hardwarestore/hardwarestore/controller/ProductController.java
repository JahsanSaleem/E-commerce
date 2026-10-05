package com.hardwarestore.hardwarestore.controller;

import com.hardwarestore.hardwarestore.model.Product;
import com.hardwarestore.hardwarestore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET all products
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/browse")
    public java.util.Map<String, Object> browse(
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="9") int size,
            @RequestParam(defaultValue="") String search,
            @RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) java.math.BigDecimal min,
            @RequestParam(required=false) java.math.BigDecimal max,
            @RequestParam(defaultValue="") String availability,
            @RequestParam(defaultValue="name") String sort) {
        return productService.browse(page, size, search, categoryId, min, max, availability, sort);
    }

    // GET product by ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    // CREATE product
    @PostMapping
    public ResponseEntity<Product> createProduct(
            @Valid @RequestBody Product product
    ) {
        Product createdProduct =
                productService.createProduct(product);

        return ResponseEntity.ok(createdProduct);
    }

    // UPDATE product
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Product product
    ) {
        Product updatedProduct =
                productService.updateProduct(id, product);

        return ResponseEntity.ok(updatedProduct);
    }

    // DELETE product
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id
    ) {
        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }
}
