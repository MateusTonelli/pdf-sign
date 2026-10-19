package com.pdfsign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tenant_members")
public class TenantMember {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantRole role;

    protected TenantMember() {
    }

    public TenantMember(UUID id, UUID tenantId, UUID userId, TenantRole role) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public TenantRole getRole() {
        return role;
    }
}
