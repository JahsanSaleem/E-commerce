package com.hardwarestore.hardwarestore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** SPA routes for the single-origin hosted build; API and OAuth paths are separate. */
@Controller
public class StorefrontController {
    @GetMapping({"/products", "/products/{id}", "/cart", "/checkout", "/orders", "/account",
            "/login", "/register", "/forgot-password", "/reset-password", "/admin", "/admin/**", "/staff", "/staff/**"})
    public String storefront() { return "forward:/index.html"; }
}
