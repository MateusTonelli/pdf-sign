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
@Table(name = "document_signers")
public class DocumentSigner {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "access_token", nullable = false, unique = true)
    private String accessToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignerStatus status;

    @Column(name = "signed_at")
    private Instant signedAt;

    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected DocumentSigner() {
    }

    public DocumentSigner(
            UUID id,
            UUID documentId,
            String name,
            String email,
            String accessToken,
            Instant tokenExpiresAt,
            SignerStatus status,
            int sortOrder) {
        this.id = id;
        this.documentId = documentId;
        this.name = name;
        this.email = email;
        this.accessToken = accessToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.status = status;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public SignerStatus getStatus() {
        return status;
    }

    public void setStatus(SignerStatus status) {
        this.status = status;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
