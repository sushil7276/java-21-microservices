package com.ecommerce.user.config;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;

/** Creates the default administrator (admin / admin123) on first start. */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private final UserRepository repository;

    public AdminSeeder(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (!repository.existsByUsername("admin")) {
            repository.save(User.builder()
                    .username("admin")
                    .email("admin@ecommerce.local")
                    .password(new BCryptPasswordEncoder().encode("admin123"))
                    .role(Role.ADMIN)
                    .createdAt(Instant.now())
                    .build());
            log.info("Default admin user created (admin / admin123)");
        }
    }
}
