package com.ecommerce.product.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;

/**
 * Loads a starter catalogue into an EMPTY product table so the shop is not blank on first start
 * (turn off with app.seed-data=false). Prices are in Indian rupees. Pictures are links to openly licensed
 * photos on Wikimedia Commons; the admin can change or remove any of this in the admin panel.
 * Nothing is seeded when the table already has products, so deleting everything in the admin panel
 * leaves the shop empty until the next restart of an empty database.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class ProductSeeder implements CommandLineRunner {

    private static final String COMMONS = "https://thumb.wikimedia.org/wikipedia/commons/thumb/";

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
                p("Apple iPhone 15 Pro", "6.1-inch Super Retina XDR display, A17 Pro chip, titanium design, 48 MP main camera.",
                        "134900", 25, "Mobiles", COMMONS + "1/19/Apple_iPhone_15_Pro.jpg/960px-Apple_iPhone_15_Pro.jpg"),
                p("Samsung Galaxy S24 Ultra", "6.8-inch Dynamic AMOLED 2X display, 200 MP camera, built-in S Pen, Galaxy AI features.",
                        "129999", 30, "Mobiles", COMMONS + "9/90/SAMSUNG_Galaxy_S24_Ultra_%285%29.jpg/960px-SAMSUNG_Galaxy_S24_Ultra_%285%29.jpg"),
                p("Apple MacBook Air M2", "13.6-inch Liquid Retina display, Apple M2 chip, up to 18 hours of battery life.",
                        "99900", 20, "Laptops", COMMONS + "d/dc/MacBook_Air_%28A2681%29_front.jpg/960px-MacBook_Air_%28A2681%29_front.jpg"),
                p("Dell XPS 13", "13.4-inch InfinityEdge display, Intel Core processor, 16 GB RAM, 512 GB SSD, slim aluminium body.",
                        "114990", 15, "Laptops", COMMONS + "8/8f/Dell_XPS_13_%282018%29.png/960px-Dell_XPS_13_%282018%29.png"),
                p("Apple AirPods Pro (2nd generation)", "Active noise cancellation, adaptive transparency, spatial audio, MagSafe charging case.",
                        "24900", 60, "Audio", COMMONS + "2/2f/AirPods_Pro_%282nd_generation%29.jpg/960px-AirPods_Pro_%282nd_generation%29.jpg"),
                p("JBL Flip 4 Bluetooth Speaker", "Waterproof portable speaker with powerful stereo sound and 12 hours of playtime.",
                        "7999", 80, "Audio", COMMONS + "a/a2/JBL_Flip_4.jpg/960px-JBL_Flip_4.jpg"),
                // no photo on purpose: the shop shows a category tile when a product has no picture
                p("Sony WH-1000XM5 Headphones", "Industry-leading noise cancelling wireless headphones with 30 hours of battery.",
                        "29990", 40, "Audio", null),
                p("Logitech MX Master 3S Mouse", "Wireless performance mouse with quiet clicks, 8K DPI sensor and multi-device pairing.",
                        "10995", 100, "Accessories", COMMONS + "b/bc/Logitech_MX_Master_3S_HS11.jpg/960px-Logitech_MX_Master_3S_HS11.jpg"),
                p("Apple Watch Series 7", "Always-on Retina display, fitness and sleep tracking, water resistant, ECG and blood oxygen.",
                        "35900", 35, "Accessories", COMMONS + "8/8d/Apple_Watch_Series_7%3B_January_2022_%2801%29.jpg/960px-Apple_Watch_Series_7%3B_January_2022_%2801%29.jpg"),
                p("Apple iPad Air", "10.9-inch Liquid Retina display, Apple M1 chip, supports Apple Pencil and Magic Keyboard.",
                        "59900", 22, "Tablets", COMMONS + "8/8d/IPad_Air.png/960px-IPad_Air.png")));
    }

    private Product p(String name, String desc, String price, int stock, String category, String imageUrl) {
        return Product.builder().name(name).description(desc).price(new BigDecimal(price))
                .stock(stock).category(category).imageUrl(imageUrl).createdAt(Instant.now()).build();
    }
}
