package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.config.CatalogueAuthorizationInterceptor;
import com.hardwarestore.hardwarestore.controller.CategoryController;
import com.hardwarestore.hardwarestore.controller.ProductController;
import com.hardwarestore.hardwarestore.exception.GlobalExceptionHandler;
import com.hardwarestore.hardwarestore.model.Category;
import com.hardwarestore.hardwarestore.model.Product;
import com.hardwarestore.hardwarestore.model.Role;
import com.hardwarestore.hardwarestore.repository.CategoryRepository;
import com.hardwarestore.hardwarestore.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogueDeletionTests {
    private ProductRepository products;
    private CategoryRepository categories;
    private Product product;
    private Category category;
    private MockMvc mvc;
    private MockHttpSession admin;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        categories = mock(CategoryRepository.class);
        product = new Product();
        category = new Category();
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(categories.findById(1L)).thenReturn(Optional.of(category));
        mvc = MockMvcBuilders.standaloneSetup(
                new ProductController(new ProductService(products, categories)),
                new CategoryController(new CategoryService(categories)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new CatalogueAuthorizationInterceptor()).build();
        admin = new MockHttpSession();
        admin.setAttribute("userId", 1L);
        admin.setAttribute("role", Role.ADMIN);
    }

    private void constraintFailure(String type, boolean duringFlush) {
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "Sensitive SQL/constraint details must not appear in the response");
        if (type.equals("products")) {
            if (duringFlush) doThrow(failure).when(products).flush();
            else doThrow(failure).when(products).delete(product);
        } else {
            if (duringFlush) doThrow(failure).when(categories).flush();
            else doThrow(failure).when(categories).delete(category);
        }
    }

    private void expectConflict(String type) throws Exception {
        String message = type.equals("products")
                ? "Cannot delete this product because it is used in a cart or order. Remove it from carts first; products in order history must be kept."
                : "Cannot delete this category because it contains products. Move or delete those products first.";
        mvc.perform(delete("/api/" + type + "/1").session(admin))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"message\":\"" + message + "\"}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"products", "categories"})
    void referencedDeletionReturnsFriendlyConflict(String type) throws Exception {
        constraintFailure(type, false);
        expectConflict(type);
    }

    @ParameterizedTest
    @ValueSource(strings = {"products", "categories"})
    void deferredDatabaseConstraintReturnsFriendlyConflict(String type) throws Exception {
        constraintFailure(type, true);
        expectConflict(type);
    }

    @ParameterizedTest
    @ValueSource(strings = {"products", "categories"})
    void unreferencedDeletionStillSucceeds(String type) throws Exception {
        mvc.perform(delete("/api/" + type + "/1").session(admin))
                .andExpect(status().isNoContent());
        if (type.equals("products")) {
            verify(products).delete(product);
            verify(products).flush();
        } else {
            verify(categories).delete(category);
            verify(categories).flush();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"products", "categories"})
    void missingRecordStillReturnsNotFound(String type) throws Exception {
        when(products.findById(999L)).thenReturn(Optional.empty());
        when(categories.findById(999L)).thenReturn(Optional.empty());
        mvc.perform(delete("/api/" + type + "/999").session(admin))
                .andExpect(status().isNotFound());
        verify(products, never()).delete(any(Product.class));
        verify(categories, never()).delete(any());
    }
}
