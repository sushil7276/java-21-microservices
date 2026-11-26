package com.ecommerce.favourite.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.favourite.exception.NotFoundException;
import com.ecommerce.favourite.client.ProductClient;
import com.ecommerce.favourite.client.ProductClient.ProductDto;
import com.ecommerce.favourite.entity.Favourite;
import com.ecommerce.favourite.dto.FavouriteResponse;
import com.ecommerce.favourite.mapper.FavouriteMapper;
import com.ecommerce.favourite.repository.FavouriteRepository;

@Service
public class FavouriteService {

    private static final Logger log = LoggerFactory.getLogger(FavouriteService.class);

    private final FavouriteRepository repository;
    private final ProductClient productClient;
    private final FavouriteMapper mapper;

    public FavouriteService(FavouriteRepository repository, ProductClient productClient, FavouriteMapper mapper) {
        this.repository = repository;
        this.productClient = productClient;
        this.mapper = mapper;
    }

    public FavouriteResponse add(Long userId, Long productId) {
        ProductDto product = productClient.getProduct(productId);          // 404 when the product does not exist
        Favourite fav = repository.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> repository.save(Favourite.builder()
                        .userId(userId).productId(productId).createdAt(Instant.now()).build()));
        return mapper.toResponse(fav, product);
    }

    public List<FavouriteResponse> list(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(f -> {
            ProductDto product = null;
            try {
                product = productClient.getProduct(f.getProductId());
            } catch (RuntimeException e) {
                log.warn("Could not load product {} for favourites: {}", f.getProductId(), e.getMessage());
            }
            return mapper.toResponse(f, product);
        }).toList();
    }

    public void remove(Long userId, Long productId) {
        Favourite fav = repository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " is not in your favourites"));
        repository.delete(fav);
    }
}
