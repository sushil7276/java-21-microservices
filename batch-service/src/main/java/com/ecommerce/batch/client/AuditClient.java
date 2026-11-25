package com.ecommerce.batch.client;

import java.util.Map;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.common.resilience.ResilientCaller;

/** batch-service ===> audit-service */
@Component
public class AuditClient {

    private final RestClient http;
    private final ResilientCaller caller;

    public AuditClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://audit-service").build();
        this.caller = caller;
    }

    /** Deletes the audit events older than {@code days} days. Returns how many were deleted. */
    public long deleteOld(int days) {
        Map<String, Number> answer = caller.call("audit-service", "delete audit events older than " + days + " days",
                () -> http.delete().uri("/internal/audit/old?days={d}", days).retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Number>>() { }));
        return answer == null ? 0 : answer.getOrDefault("deleted", 0).longValue();
    }
}
