package com.ecommerce.audit.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ecommerce.audit.entity.AuditLog;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findTop100ByOrderByReceivedAtDesc();

    /** Deletes every event received before the given time and returns how many were deleted. */
    long deleteByReceivedAtBefore(Instant cutoff);

    List<AuditLog> findByEntityIdOrderByReceivedAtAsc(String entityId);

    List<AuditLog> findTop100ByEventTypeOrderByReceivedAtDesc(String eventType);

    List<AuditLog> findTop100BySourceOrderByReceivedAtDesc(String source);
}
