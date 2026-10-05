package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.exception.ResourceNotFoundException;
import com.hardwarestore.hardwarestore.exception.ResourceConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import com.hardwarestore.hardwarestore.model.Category;
import com.hardwarestore.hardwarestore.model.Product;
import com.hardwarestore.hardwarestore.repository.CategoryRepository;
import com.hardwarestore.hardwarestore.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // Get all products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public java.util.Map<String, Object> browse(int page, int size, String search, Long categoryId,
            java.math.BigDecimal min, java.math.BigDecimal max, String availability, String sort) {
        if (page < 1 || size < 1 || size > 100 || search.length() > 255 ||
                (min != null && min.signum() < 0) || (max != null && max.signum() < 0) ||
                (min != null && max != null && min.compareTo(max) > 0))
            throw new IllegalArgumentException("Invalid page or price filters.");
        if (!java.util.Set.of("", "in", "out").contains(availability))
            throw new IllegalArgumentException("Invalid availability filter.");
        var ordering = switch (sort) {
            case "name" -> org.springframework.data.domain.Sort.by("name").ascending();
            case "price-low" -> org.springframework.data.domain.Sort.by("price").ascending();
            case "price-high" -> org.springframework.data.domain.Sort.by("price").descending();
            default -> throw new IllegalArgumentException("Invalid product sort.");
        };
        ordering = ordering.and(org.springframework.data.domain.Sort.by("productId"));
        org.springframework.data.jpa.domain.Specification<Product> filters = (root, query, cb) -> {
            var rules = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!search.isBlank()) {
                String term = "%" + search.trim().toLowerCase(java.util.Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                rules.add(cb.or(cb.like(cb.lower(root.get("name")), term, '!'),
                        cb.like(cb.lower(root.get("description")), term, '!')));
            }
            if (categoryId != null) rules.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            if (min != null) rules.add(cb.greaterThanOrEqualTo(root.get("price"), min));
            if (max != null) rules.add(cb.lessThanOrEqualTo(root.get("price"), max));
            if (availability.equals("in")) rules.add(cb.greaterThan(root.get("quantity"), 0));
            if (availability.equals("out")) rules.add(cb.equal(root.get("quantity"), 0));
            return cb.and(rules.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = productRepository.findAll(filters, org.springframework.data.domain.PageRequest.of(page-1, size, ordering));
        int pages = Math.max(1, result.getTotalPages());
        if (page > pages) result = productRepository.findAll(filters, org.springframework.data.domain.PageRequest.of(pages-1, size, ordering));
        return java.util.Map.of("content", result.getContent(), "totalElements", result.getTotalElements(),
                "totalPages", pages, "page", Math.min(page, pages), "size", size);
    }

    // Get product by ID
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        ));
    }

    // Create product
    public Product createProduct(Product product) {

        Category category = getProductCategory(product);

        product.setCategory(category);

        return productRepository.save(product);
    }

    // Update product
    public Product updateProduct(Long id, Product updatedProduct) {

        Product existingProduct = getProductById(id);

        existingProduct.setName(updatedProduct.getName());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setImageUrl(updatedProduct.getImageUrl());
        existingProduct.setQuantity(updatedProduct.getQuantity());

        Category category = getProductCategory(updatedProduct);

        existingProduct.setCategory(category);

        return productRepository.save(existingProduct);
    }

    // Delete product
    @Transactional
    public void deleteProduct(Long id) {

        Product existingProduct = getProductById(id);

        try {
            productRepository.delete(existingProduct);
            // Force database constraints to be checked before leaving this method.
            productRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException(
                    "Cannot delete this product because it is used in a cart or order. Remove it from carts first; products in order history must be kept.", exception);
        }
    }

    private Category getProductCategory(Product product) {

        if (product.getCategory() == null ||
                product.getCategory().getCategoryId() == null) {
            throw new IllegalArgumentException(
                    "A valid category id is required for the product"
            );
        }

        Long categoryId = product.getCategory().getCategoryId();

        return categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: " + categoryId
                        ));
    }
}
