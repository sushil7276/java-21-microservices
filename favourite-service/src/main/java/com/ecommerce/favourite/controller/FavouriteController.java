package com.ecommerce.favourite.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.common.web.Headers;
import com.ecommerce.favourite.dto.FavouriteResponse;
import com.ecommerce.favourite.service.FavouriteService;

@RestController
public class FavouriteController {

    private final FavouriteService service;

    public FavouriteController(FavouriteService service) {
        this.service = service;
    }

    @GetMapping("/api/favourites")
    public List<FavouriteResponse> list(@RequestHeader(Headers.USER_ID) Long userId) {
        return service.list(userId);
    }

    @PostMapping("/api/favourites/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FavouriteResponse add(@RequestHeader(Headers.USER_ID) Long userId, @PathVariable Long productId) {
        return service.add(userId, productId);
    }

    @DeleteMapping("/api/favourites/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@RequestHeader(Headers.USER_ID) Long userId, @PathVariable Long productId) {
        service.remove(userId, productId);
    }
}
