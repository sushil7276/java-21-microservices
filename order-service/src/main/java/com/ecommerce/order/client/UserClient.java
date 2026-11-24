package com.ecommerce.order.client;


import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.common.resilience.ResilientCaller;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** order-service ===> user-service : who is buying? */
@Component
public class UserClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserDto(Long id, String username, String email) {
    }

    private final RestClient http;
    private final ResilientCaller caller;

    public UserClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://user-service").build();
        this.caller = caller;
    }

    public UserDto getUser(Long userId) {
        return caller.call("user-service", "get user " + userId, () -> {
            try {
                return http.get().uri("/internal/users/{id}", userId).retrieve().body(UserDto.class);
            } catch (HttpClientErrorException.NotFound e) {
                throw new NotFoundException("User not found: " + userId);
            }
        });
    }
}
