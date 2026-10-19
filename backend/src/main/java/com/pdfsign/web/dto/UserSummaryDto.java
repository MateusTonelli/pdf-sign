package com.pdfsign.web.dto;

import java.util.UUID;

public record UserSummaryDto(
        UUID id, String name, String email, UUID tenantId, String tenantName, boolean hasCertificate) {
}
