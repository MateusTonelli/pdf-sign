package com.pdfsign.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateMetadataDto(
        UUID id, String cn, Instant validFrom, Instant validTo, String thumbprint, boolean active) {
}
