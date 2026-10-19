package com.pdfsign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_email")
    private String actorEmail;

    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private String ip;

    protected AuditEvent() {
    }

    public AuditEvent(
            UUID id,
            UUID documentId,
            UUID tenantId,
            String eventType,
            String actorEmail,
            String details,
            Instant createdAt,
            String ip) {
        this.id = id;
        this.documentId = documentId;
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.actorEmail = actorEmail;
        this.details = details;
        this.createdAt = createdAt;
        this.ip = ip;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getIp() {
        return ip;
    }
}
