package com.mybakery.repository;

import com.mybakery.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Product} entities.
 * JpaRepository provides CRUD methods (save, findById, delete, etc.) out of the box.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Find all products in a given category, ordered by name. */
    List<Product> findByCategoryOrderByNameAsc(String category);

    /** Find only products that are currently available. */
    List<Product> findByAvailableTrueOrderByNameAsc();

    Optional<Product> findByIdAndAvailableTrue(Long id);
}
