package com.fashion.backend.modules.user.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String role,
        String gender,
        LocalDate dateOfBirth,
        LocalDateTime createdAt
) {
}