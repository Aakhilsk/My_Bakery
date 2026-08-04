package com.mybakery.service;

import com.mybakery.model.Product;
import com.mybakery.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Service layer — contains business logic and sits between controllers and repositories.
 * {@code @Transactional} ensures database operations run inside a transaction.
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    /** Constructor injection: Spring provides the repository automatically. */
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Returns every product, sorted alphabetically by name. */
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll()
                .stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    /** Returns only products available for sale — used by the public storefront. */
    @Transactional(readOnly = true)
    public List<Product> findAvailable() {
        return productRepository.findByAvailableTrueOrderByNameAsc();
    }

    /** Groups available products by category for the public menu display. */
    @Transactional(readOnly = true)
    public Map<String, List<Product>> findAvailableGroupedByCategory() {
        return findAvailable().stream()
                .collect(Collectors.groupingBy(
                        Product::getCategory,
                        TreeMap::new,
                        Collectors.toList()
                ));
    }

    /** Finds a single product by ID, or throws if not found. */
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    /** Finds a public product without exposing items that are not for sale. */
    @Transactional(readOnly = true)
    public Product findAvailableById(Long id) {
        return productRepository.findByIdAndAvailableTrue(id)
                .orElseThrow(() -> new ProductNotFoundException("Available product not found with id: " + id));
    }

    /** Persists a new product or updates an existing one. */
    public Product save(Product product) {
        return productRepository.save(product);
    }

    /**
     * Updates an existing product with new field values.
     * Only non-null fields from the incoming product are applied.
     */
    public Product update(Long id, Product updatedProduct) {
        Product existing = findById(id);

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setCategory(updatedProduct.getCategory());
        existing.setAvailable(updatedProduct.isAvailable());
        if (updatedProduct.getImagePath() != null && !updatedProduct.getImagePath().isBlank()) {
            existing.setImagePath(updatedProduct.getImagePath());
        }

        return productRepository.save(existing);
    }

    /** Flips whether a product is visible on the public storefront. */
    public Product toggleAvailability(Long id) {
        Product product = findById(id);
        product.setAvailable(!product.isAvailable());
        return productRepository.save(product);
    }

    /** Deletes a product by ID after verifying it exists. */
    public void deleteById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}
