CREATE TABLE users (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tenants (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tenant_members (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants (id),
    user_id         UUID NOT NULL REFERENCES users (id),
    role            VARCHAR(32) NOT NULL,
    CONSTRAINT uq_tenant_member UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_tenant_members_user ON tenant_members (user_id);

CREATE TABLE user_certificates (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id),
    encrypted_pkcs12    BYTEA NOT NULL,
    cn                  VARCHAR(512),
    valid_from          TIMESTAMPTZ,
    valid_to            TIMESTAMPTZ,
    thumbprint          VARCHAR(128),
    active              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_user_certificates_user_active ON user_certificates (user_id, active);

CREATE TABLE documents (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants (id),
    created_by_user_id  UUID NOT NULL REFERENCES users (id),
    title               VARCHAR(512) NOT NULL,
    storage_path        VARCHAR(1024) NOT NULL,
    content_hash_sha256 VARCHAR(64) NOT NULL,
    signature_type      VARCHAR(32) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_documents_tenant ON documents (tenant_id);

CREATE TABLE document_signers (
    id              UUID PRIMARY KEY,
    document_id     UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    access_token    VARCHAR(512) NOT NULL UNIQUE,
    token_expires_at TIMESTAMPTZ,
    status          VARCHAR(32) NOT NULL,
    signed_at       TIMESTAMPTZ,
    ip              VARCHAR(64),
    user_agent      TEXT,
    sort_order      INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_document_signers_document ON document_signers (document_id);
CREATE INDEX idx_document_signers_token ON document_signers (access_token);

CREATE TABLE signature_fields (
    id              UUID PRIMARY KEY,
    document_id     UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    signer_id       UUID NOT NULL REFERENCES document_signers (id) ON DELETE CASCADE,
    page            INT NOT NULL,
    x               DOUBLE PRECISION NOT NULL,
    y               DOUBLE PRECISION NOT NULL,
    width           DOUBLE PRECISION NOT NULL,
    height          DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_signature_fields_document ON signature_fields (document_id);

CREATE TABLE audit_events (
    id              UUID PRIMARY KEY,
    document_id     UUID,
    tenant_id       UUID,
    event_type      VARCHAR(64) NOT NULL,
    actor_email     VARCHAR(255),
    details         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip              VARCHAR(64)
);

CREATE INDEX idx_audit_events_document ON audit_events (document_id);
CREATE INDEX idx_audit_events_tenant ON audit_events (tenant_id);
