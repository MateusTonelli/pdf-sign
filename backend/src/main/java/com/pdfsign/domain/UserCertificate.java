package com.pdfsign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_certificates")
public class UserCertificate {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Lob
    @Column(name = "encrypted_pkcs12", nullable = false)
    private byte[] encryptedPkcs12;

    private String cn;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    private String thumbprint;

    @Column(nullable = false)
    private boolean active;

    protected UserCertificate() {
    }

    public UserCertificate(
            UUID id,
            UUID userId,
            byte[] encryptedPkcs12,
            String cn,
            Instant validFrom,
            Instant validTo,
            String thumbprint,
            boolean active) {
        this.id = id;
        this.userId = userId;
        this.encryptedPkcs12 = encryptedPkcs12;
        this.cn = cn;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.thumbprint = thumbprint;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public byte[] getEncryptedPkcs12() {
        return encryptedPkcs12;
    }

    public String getCn() {
        return cn;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public String getThumbprint() {
        return thumbprint;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
