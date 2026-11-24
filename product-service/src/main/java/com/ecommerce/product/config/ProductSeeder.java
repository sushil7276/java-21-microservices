package com.ecommerce.product.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;

/** Loads a few demo products into an empty catalogue so the flow can be tested immediately. */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class ProductSeeder implements CommandLineRunner {

    private final ProductRepository repository;

    public ProductSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                p("iPhone 15", "Apple smartphone 128GB", "799.00", 50, "Mobiles"),
                p("Galaxy S24", "Samsung smartphone 256GB", "699.00", 40, "Mobiles"),
                p("MacBook Air M3", "13 inch laptop", "1199.00", 25, "Laptops"),
                p("Sony WH-1000XM5", "Noise cancelling headphones", "349.00", 100, "Audio"),
                p("Logitech MX Master 3S", "Wireless mouse", "99.00", 200, "Accessories")));
    }

    private Product p(String name, String desc, String price, int stock, String category) {
        return Product.builder().name(name).description(desc).price(new BigDecimal(price))
                .stock(stock).category(category).createdAt(Instant.now()).build();
    }
}
