package com.ecommerce.product.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Validator;

import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.product.config.CacheConfig;
import com.ecommerce.product.dto.BulkResult;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final Validator validator;

    public ProductService(ProductRepository repository, ProductMapper mapper, Validator validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
    }

    // @Cacheable: answer from Redis if it is there; otherwise run the method and store the result in Redis.
    // The key contains both filters, so every different search has its own cached answer.
    @Cacheable(cacheNames = CacheConfig.PRODUCT_LIST, key = "'category=' + #category + ',name=' + #name")
    public List<ProductResponse> search(String category, String name) {
        List<Product> found;
        if (category != null && !category.isBlank()) {
            found = repository.findByCategoryIgnoreCase(category);
        } else if (name != null && !name.isBlank()) {
            found = repository.findByNameContainingIgnoreCase(name);
        } else {
            found = repository.findAll();
        }
        return mapper.toResponses(found);
    }

    /**
     * Creates many products at once (used by the CSV import batch job in batch-service).
     * Each product is checked with the same rules as a single create; a bad row or a name that already exists is
     * reported back and skipped, it never stops the others.
     */
    @CacheEvict(cacheNames = {CacheConfig.PRODUCT, CacheConfig.PRODUCT_LIST}, allEntries = true)
    @Transactional
    public BulkResult bulkCreate(List<ProductRequest> requests) {
        List<BulkResult.Rejected> rejected = new ArrayList<>();
        Set<String> namesInThisBatch = new HashSet<>();
        int created = 0;
        for (int i = 0; i < requests.size(); i++) {
            ProductRequest request = requests.get(i);
            String problem = validate(request);
            if (problem == null && (!namesInThisBatch.add(request.name().trim().toLowerCase())
                    || repository.existsByNameIgnoreCase(request.name().trim()))) {
                problem = "a product with this name already exists";
            }
            if (problem != null) {
                rejected.add(new BulkResult.Rejected(i, request.name(), problem));
                continue;
            }
            repository.save(mapper.toEntity(request));
            created++;
        }
        return new BulkResult(created, rejected);
    }

    /** @return the first validation problem as text, or null when the product is fine */
    private String validate(ProductRequest request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .sorted().findFirst().orElse(null);
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCT, key = "#id")
    public ProductResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    // @CacheEvict: any change makes the cached answers wrong, so we simply delete them all; the next read refills Redis.
    @CacheEvict(cacheNames = {CacheConfig.PRODUCT, CacheConfig.PRODUCT_LIST}, allEntries = true)
    public ProductResponse create(ProductRequest request) {
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }

    @CacheEvict(cacheNames = {CacheConfig.PRODUCT, CacheConfig.PRODUCT_LIST}, allEntries = true)
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        mapper.updateEntity(request, product);          // saved automatically at the end of the transaction
        return mapper.toResponse(product);
    }

    @CacheEvict(cacheNames = {CacheConfig.PRODUCT, CacheConfig.PRODUCT_LIST}, allEntries = true)
    public void delete(Long id) {
        repository.delete(find(id));
    }

    /** Called by order-service. One SQL statement, so two buyers can never oversell the last item. */
    // Stock is part of what the shop shows, so buying something also clears the cache.
    @CacheEvict(cacheNames = {CacheConfig.PRODUCT, CacheConfig.PRODUCT_LIST}, allEntries = true)
    @Transactional
    public ProductResponse reduceStock(Long id, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        Product product = find(id);
        if (repository.reduceStock(id, quantity) == 0) {
            throw new ConflictException("Insufficient stock for product " + id + " (available: " + product.getStock() + ")");
        }
        return mapper.toResponse(find(id));
    }

    /** Called by order-service to undo a reservation when payment fails. */
    @CacheEvict(cacheNames = {CacheConfig.PRODUCT, CacheConfig.PRODUCT_LIST}, allEntries = true)
    @Transactional
    public ProductResponse restoreStock(Long id, int quantity) {
        find(id);
        repository.restoreStock(id, quantity);
        return mapper.toResponse(find(id));
    }

    private Product find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }
}
