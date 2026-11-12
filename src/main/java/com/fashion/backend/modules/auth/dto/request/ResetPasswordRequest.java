package com.fashion.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email không được để trống")
        String email,
        @NotBlank(message = "Mã xác minh không được để trống")
        @Pattern(regexp = "\\d{6}", message = "Mã xác minh phải gồm 6 chữ số")
        String code,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
        String newPassword
) {
}
