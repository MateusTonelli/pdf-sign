package com.pdfsign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(nullable = false)
    private String title;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "content_hash_sha256", nullable = false)
    private String contentHashSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false)
    private SignatureType signatureType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Document() {
    }

    public Document(
            UUID id,
            UUID tenantId,
            UUID createdByUserId,
            String title,
            String storagePath,
            String contentHashSha256,
            SignatureType signatureType,
            DocumentStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.createdByUserId = createdByUserId;
        this.title = title;
        this.storagePath = storagePath;
        this.contentHashSha256 = contentHashSha256;
        this.signatureType = signatureType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getContentHashSha256() {
        return contentHashSha256;
    }

    public SignatureType getSignatureType() {
        return signatureType;
    }

    public void setSignatureType(SignatureType signatureType) {
        this.signatureType = signatureType;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
