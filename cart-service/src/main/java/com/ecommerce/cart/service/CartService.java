package com.ecommerce.cart.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.cart.client.ProductClient;
import com.ecommerce.cart.client.ProductClient.ProductDto;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.dto.Dtos.AddItemRequest;
import com.ecommerce.cart.dto.Dtos.CartItemResponse;
import com.ecommerce.cart.dto.Dtos.CartResponse;
import com.ecommerce.cart.mapper.CartMapper;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.NotFoundException;

@Service
public class CartService {

    private final CartRepository repository;
    private final ProductClient productClient;
    private final CartMapper mapper;

    public CartService(CartRepository repository, ProductClient productClient, CartMapper mapper) {
        this.repository = repository;
        this.productClient = productClient;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CartResponse get(Long userId) {
        return repository.findByUserId(userId).map(this::toResponse)
                .orElseGet(() -> new CartResponse(userId, List.of(), BigDecimal.ZERO));
    }

    @Transactional
    public CartResponse addItem(Long userId, AddItemRequest req) {
        ProductDto product = productClient.getProduct(req.productId());   // price is always taken from product-service
        Cart cart = repository.findByUserId(userId).orElseGet(() -> new Cart(userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(req.productId())).findFirst().orElse(null);
        int newQuantity = (item == null ? 0 : item.getQuantity()) + req.quantity();
        if (product.stock() != null && newQuantity > product.stock()) {
            throw new ConflictException("Only " + product.stock() + " units of '" + product.name() + "' in stock");
        }
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProductId(product.id());
            cart.getItems().add(item);
        }
        item.setProductName(product.name());
        item.setUnitPrice(product.price());
        item.setQuantity(newQuantity);
        cart.setUpdatedAt(Instant.now());
        return toResponse(repository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = repository.findByUserId(userId).orElseThrow(() -> new NotFoundException("Cart is empty"));
        if (!cart.getItems().removeIf(i -> i.getProductId().equals(productId))) {
            throw new NotFoundException("Product " + productId + " is not in the cart");
        }
        cart.setUpdatedAt(Instant.now());
        return toResponse(repository.save(cart));
    }

    @Transactional
    public void clear(Long userId) {
        repository.findByUserId(userId).ifPresent(repository::delete);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = mapper.toItemResponses(cart.getItems());
        BigDecimal total = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getUserId(), items, total);
    }
}
