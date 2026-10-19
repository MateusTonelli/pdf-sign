package com.pdfsign.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignerInputDto(@NotBlank String name, @NotBlank @Email String email) {
}
