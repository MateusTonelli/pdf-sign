package com.pdfsign.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignatureFieldInputDto(
        @NotBlank @Email String signerEmail,
        @NotNull Integer page,
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double width,
        @NotNull Double height) {
}
