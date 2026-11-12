package com.fashion.backend.modules.order.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SePayWebhookPayload(
        Long id,
        String accountNumber,
        String transferType,
        String code,
        String content,
        BigDecimal transferAmount,
        String transactionDate
) {
}
