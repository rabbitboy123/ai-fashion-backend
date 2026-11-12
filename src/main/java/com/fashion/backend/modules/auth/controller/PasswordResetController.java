package com.fashion.backend.modules.auth.controller;

import com.fashion.backend.common.api.ApiResponse;
import com.fashion.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.fashion.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.fashion.backend.modules.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String GENERIC_MESSAGE = "Nếu email tồn tại, mã đặt lại mật khẩu sẽ được gửi.";

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> requestCode(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestCode(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(GENERIC_MESSAGE, null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!passwordResetService.resetPassword(request)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã xác minh không hợp lệ hoặc đã hết hạn."));
        }
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công.", null));
    }
}
