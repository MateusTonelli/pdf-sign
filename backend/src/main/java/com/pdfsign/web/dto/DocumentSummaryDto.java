package com.pdfsign.web.dto;

import com.pdfsign.domain.DocumentStatus;
import com.pdfsign.domain.SignatureType;
import java.time.Instant;
import java.util.UUID;

public record DocumentSummaryDto(
        UUID id,
        String title,
        SignatureType signatureType,
        DocumentStatus status,
        String contentHashSha256,
        Instant createdAt,
        Instant updatedAt) {
}
