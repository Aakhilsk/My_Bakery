package com.mybakery.config;

import com.mybakery.model.Product;
import com.mybakery.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

/**
 * Seeds the database with sample products on first startup.
 * Runs only when the products table is empty so restarts do not duplicate data.
 */
@Configuration
@Profile("local")
public class DataInitializer {

    @Bean
    CommandLineRunner loadSampleProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            productRepository.save(new Product(
                    "Sourdough Loaf",
                    "Classic artisan sourdough with a crispy crust",
                    new BigDecimal("6.50"),
                    "Bread",
                    true
            ));
            productRepository.save(new Product(
                    "Chocolate Croissant",
                    "Buttery croissant filled with dark chocolate",
                    new BigDecimal("3.75"),
                    "Pastry",
                    true
            ));
            productRepository.save(new Product(
                    "Blueberry Muffin",
                    "Fresh blueberries in a soft muffin",
                    new BigDecimal("2.95"),
                    "Pastry",
                    true
            ));
            productRepository.save(new Product(
                    "Red Velvet Cake",
                    "Moist red velvet with cream cheese frosting",
                    new BigDecimal("28.00"),
                    "Cake",
                    true
            ));
            productRepository.save(new Product(
                    "Oatmeal Cookie",
                    "Chewy oatmeal raisin cookie",
                    new BigDecimal("1.50"),
                    "Cookie",
                    false
            ));
        };
    }
}
