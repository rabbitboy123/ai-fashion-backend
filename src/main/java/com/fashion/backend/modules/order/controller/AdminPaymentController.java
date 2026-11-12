package com.fashion.backend.modules.order.controller;

import com.fashion.backend.common.api.ApiResponse;
import com.fashion.backend.modules.order.dto.response.PaymentReconciliationResponse;
import com.fashion.backend.modules.order.service.BankPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final BankPaymentService bankPaymentService;

    @GetMapping("/reconciliation")
    public ResponseEntity<ApiResponse<List<PaymentReconciliationResponse>>> getReconciliationItems() {
        return ResponseEntity.ok(ApiResponse.success(bankPaymentService.getReconciliationItems()));
    }
}
