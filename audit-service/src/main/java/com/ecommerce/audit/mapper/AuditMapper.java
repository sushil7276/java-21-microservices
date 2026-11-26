package com.ecommerce.audit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecommerce.audit.entity.AuditLog;
import com.ecommerce.audit.event.AuditEvent;

@Mapper(componentModel = "spring")
public interface AuditMapper {

    /** Kafka message (AuditEvent) -> MongoDB document (AuditLog). */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventTime", source = "timestamp")
    @Mapping(target = "receivedAt", expression = "java(java.time.Instant.now())")
    AuditLog toDocument(AuditEvent event);
}
