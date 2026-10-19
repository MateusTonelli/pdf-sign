package com.pdfsign.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CertificateSignRequest(@NotBlank String pfxPassword) {
}
