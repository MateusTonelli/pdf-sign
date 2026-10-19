package com.pdfsign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "signature_fields")
public class SignatureField {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "signer_id", nullable = false)
    private UUID signerId;

    @Column(nullable = false)
    private int page;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double width;

    @Column(nullable = false)
    private double height;

    protected SignatureField() {
    }

    public SignatureField(UUID id, UUID documentId, UUID signerId, int page, double x, double y, double width, double height) {
        this.id = id;
        this.documentId = documentId;
        this.signerId = signerId;
        this.page = page;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getSignerId() {
        return signerId;
    }

    public int getPage() {
        return page;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
