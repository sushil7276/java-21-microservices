package com.ecommerce.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.user.exception.ForbiddenException;
import com.ecommerce.user.web.Headers;
import com.ecommerce.user.dto.Dtos.UpdateUserRequest;
import com.ecommerce.user.dto.Dtos.UserResponse;
import com.ecommerce.user.service.UserService;

import jakarta.validation.Valid;

@RestController
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/api/users/me")
    public UserResponse me(@RequestHeader(Headers.USER_ID) Long userId) {
        return service.get(userId);
    }

    @GetMapping("/api/users")
    public List<UserResponse> list() {
        return service.list();
    }

    @GetMapping("/api/users/{id}")
    public UserResponse get(@PathVariable Long id,
                            @RequestHeader(Headers.USER_ID) Long callerId,
                            @RequestHeader(Headers.USER_ROLE) String role) {
        requireSelfOrAdmin(id, callerId, role);
        return service.get(id);
    }

    @PutMapping("/api/users/{id}")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest request,
                               @RequestHeader(Headers.USER_ID) Long callerId,
                               @RequestHeader(Headers.USER_ROLE) String role) {
        requireSelfOrAdmin(id, callerId, role);
        return service.update(id, request);
    }

    @DeleteMapping("/api/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /** Service-to-service lookup (not routed through the gateway). */
    @GetMapping("/internal/users/{id}")
    public UserResponse internalGet(@PathVariable Long id) {
        return service.get(id);
    }

    private void requireSelfOrAdmin(Long id, Long callerId, String role) {
        if (!id.equals(callerId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("You can only access your own profile");
        }
    }
}
