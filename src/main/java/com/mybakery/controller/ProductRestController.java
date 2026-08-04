package com.mybakery.controller;

import com.mybakery.model.Product;
import com.mybakery.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API controller — exposes JSON endpoints under {@code /api/products}.
 * Used by external clients, Postman, or JavaScript front-ends.
 */
@RestController
@RequestMapping("/api/products")
public class ProductRestController {

    private final ProductService productService;

    public ProductRestController(ProductService productService) {
        this.productService = productService;
    }

    /** GET /api/products — list all products. */
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.findAvailable();
    }

    /** GET /api/products/{id} — get one product by ID. */
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.findAvailableById(id);
    }

    /** POST /api/products — create a new product. Returns 201 Created. */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product saved = productService.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** PUT /api/products/{id} — update an existing product. */
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        return productService.update(id, product);
    }

    /** DELETE /api/products/{id} — remove a product. Returns 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
