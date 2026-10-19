package com.pdfsign.web.dto;

import com.pdfsign.domain.SignerStatus;
import java.time.Instant;
import java.util.UUID;

public record SignerDto(
        UUID id, String name, String email, SignerStatus status, Instant signedAt, int sortOrder) {
}
