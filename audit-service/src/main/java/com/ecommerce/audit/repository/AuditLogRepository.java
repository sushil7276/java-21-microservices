package com.ecommerce.audit.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ecommerce.audit.entity.AuditLog;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findTop100ByOrderByReceivedAtDesc();

    List<AuditLog> findByEntityIdOrderByReceivedAtAsc(String entityId);

    List<AuditLog> findTop100ByEventTypeOrderByReceivedAtDesc(String eventType);

    List<AuditLog> findTop100BySourceOrderByReceivedAtDesc(String source);
}
