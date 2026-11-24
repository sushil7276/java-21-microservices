package com.ecommerce.audit.entity;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document("audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    private String id;
    @Indexed
    private String eventType;
    private String source;
    @Indexed
    private String entityId;
    private Long userId;
    private Map<String, Object> payload;
    private Instant eventTime;
    private Instant receivedAt;
}
