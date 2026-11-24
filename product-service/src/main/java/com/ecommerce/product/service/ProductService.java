package com.ecommerce.product.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    public ProductService(ProductRepository repository, ProductMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

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

    public ProductResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    public ProductResponse create(ProductRequest request) {
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        mapper.updateEntity(request, product);          // saved automatically at the end of the transaction
        return mapper.toResponse(product);
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    /** Called by order-service. One SQL statement, so two buyers can never oversell the last item. */
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
