package com.ecommerce.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.audit.mapper.AuditMapper;
import com.ecommerce.audit.repository.AuditLogRepository;
import com.ecommerce.common.event.AuditEvent;

import tools.jackson.databind.json.JsonMapper;

/** Kafka -> MongoDB: stores every business event that order-service published to the topic "audit-events". */
@Component
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final AuditLogRepository repository;
    private final AuditMapper mapper;
    private final JsonMapper json;

    public AuditConsumer(AuditLogRepository repository, AuditMapper mapper, JsonMapper json) {
        this.repository = repository;
        this.mapper = mapper;
        this.json = json;
    }

    @KafkaListener(topics = AuditEvent.TOPIC)
    public void onEvent(String message) {
        try {
            AuditEvent event = json.readValue(message, AuditEvent.class);   // JSON text -> object
            repository.save(mapper.toDocument(event));                        // object -> MongoDB document
            log.info("[audit-service] stored {} for order {}", event.eventType(), event.entityId());
        } catch (Exception e) {
            log.error("[audit-service] skipping unreadable message '{}': {}", message, e.getMessage());
        }
    }
}
