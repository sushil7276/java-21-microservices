package com.ecommerce.order.audit;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ecommerce.common.event.AuditEvent;

import tools.jackson.databind.json.JsonMapper;

/** Publishes business events to Kafka. Audit must never break the order flow, so failures are only logged. */
@Component
public class AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper mapper;

    public AuditPublisher(KafkaTemplate<String, String> kafka, JsonMapper mapper) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    public void publish(String eventType, Long orderId, Long userId, Map<String, Object> payload) {
        try {
            AuditEvent event = new AuditEvent(eventType, "order-service", String.valueOf(orderId), userId,
                    payload, Instant.now());
            kafka.send(AuditEvent.TOPIC, String.valueOf(orderId), mapper.writeValueAsString(event))
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            log.warn("Could not publish {} to Kafka: {}", eventType, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Could not publish {} to Kafka: {}", eventType, e.getMessage());
        }
    }
}
