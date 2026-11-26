package com.ecommerce.order.event;

import java.time.Instant;
import java.util.Map;

/** Event contract published to Kafka by business services and consumed by audit-service. */
public record AuditEvent(String eventType,
                         String source,
                         String entityId,
                         Long userId,
                         Map<String, Object> payload,
                         Instant timestamp) {

    public static final String TOPIC = "audit-events";
}
