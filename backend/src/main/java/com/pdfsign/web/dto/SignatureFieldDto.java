package com.pdfsign.web.dto;

import java.util.UUID;

public record SignatureFieldDto(
        UUID id, String signerEmail, int page, double x, double y, double width, double height) {
}
