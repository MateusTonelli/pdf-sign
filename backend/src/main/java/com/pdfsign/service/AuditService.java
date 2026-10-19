package com.pdfsign.service;

import com.pdfsign.domain.AuditEvent;
import com.pdfsign.repository.AuditEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(UUID documentId, UUID tenantId, String eventType, String actorEmail, String details, String ip) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                documentId,
                tenantId,
                eventType,
                actorEmail,
                details,
                Instant.now(),
                ip);
        auditEventRepository.save(event);
    }
}
