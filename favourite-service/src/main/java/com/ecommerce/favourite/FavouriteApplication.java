package com.ecommerce.favourite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ecommerce.favourite", "com.ecommerce.common"})
public class FavouriteApplication {
    public static void main(String[] args) {
        SpringApplication.run(FavouriteApplication.class, args);
    }
}
