package com.ecommerce.favourite.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.favourite.entity.Favourite;

public interface FavouriteRepository extends JpaRepository<Favourite, Long> {
    List<Favourite> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Favourite> findByUserIdAndProductId(Long userId, Long productId);
}
