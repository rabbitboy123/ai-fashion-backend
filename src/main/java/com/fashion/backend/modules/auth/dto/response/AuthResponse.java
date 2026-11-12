package com.fashion.backend.modules.auth.dto.response;


public record AuthResponse(
        String accessToken,
        String tokenType,
        Long id,
        String email,
        String fullName,
        String role
) {
    public AuthResponse {
        tokenType = tokenType == null ? "Bearer" : tokenType;
    }
}