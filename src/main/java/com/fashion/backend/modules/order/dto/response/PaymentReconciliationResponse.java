package com.fashion.backend.modules.order.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentReconciliationResponse(
        Long transactionId,
        Long sepayTransactionId,
        Long orderId,
        String paymentReference,
        String accountNumber,
        String transferType,
        String paymentCode,
        String content,
        BigDecimal transferAmount,
        String transactionDate,
        LocalDateTime receivedAt
) {
}
