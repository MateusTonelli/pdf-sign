package com.pdfsign.web.dto;

public record AuthResponse(String accessToken, String refreshToken, UserSummaryDto user) {
}
