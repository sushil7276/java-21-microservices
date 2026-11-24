package com.ecommerce.audit.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.audit.entity.AuditLog;
import com.ecommerce.audit.repository.AuditLogRepository;

/** Read-only audit trail (ADMIN only, enforced at the gateway). */
@RestController
public class AuditController {

    private final AuditLogRepository repository;

    public AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/audit")
    public List<AuditLog> search(@RequestParam(required = false) String eventType,
                                 @RequestParam(required = false) String source,
                                 @RequestParam(required = false) String entityId) {
        if (entityId != null) {
            return repository.findByEntityIdOrderByReceivedAtAsc(entityId);
        }
        if (eventType != null) {
            return repository.findTop100ByEventTypeOrderByReceivedAtDesc(eventType);
        }
        if (source != null) {
            return repository.findTop100BySourceOrderByReceivedAtDesc(source);
        }
        return repository.findTop100ByOrderByReceivedAtDesc();
    }

    @GetMapping("/api/audit/orders/{orderId}")
    public List<AuditLog> forOrder(@PathVariable String orderId) {
        return repository.findByEntityIdOrderByReceivedAtAsc(orderId);
    }
}
