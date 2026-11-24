package com.ecommerce.product.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.service.ProductService;

import jakarta.validation.Valid;

@RestController
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/api/products")
    public List<ProductResponse> list(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String name) {
        return service.search(category, name);
    }

    @GetMapping("/api/products/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/api/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    @PutMapping("/api/products/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ---- internal endpoints used by order-service (not routed through the gateway) ----

    @PostMapping("/internal/products/{id}/reduce-stock")
    public ProductResponse reduceStock(@PathVariable Long id, @RequestParam int quantity) {
        return service.reduceStock(id, quantity);
    }

    @PostMapping("/internal/products/{id}/restore-stock")
    public ProductResponse restoreStock(@PathVariable Long id, @RequestParam int quantity) {
        return service.restoreStock(id, quantity);
    }
}
