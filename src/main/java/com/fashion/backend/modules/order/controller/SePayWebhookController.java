package com.fashion.backend.modules.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fashion.backend.modules.order.service.BankPaymentService;
import com.fashion.backend.modules.order.service.SePayWebhookPayload;
import com.fashion.backend.modules.order.service.SePayWebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/webhooks")
@RequiredArgsConstructor
public class SePayWebhookController {

    private final SePayWebhookSignatureVerifier signatureVerifier;
    private final BankPaymentService bankPaymentService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/sepay", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature) {
        if (!signatureVerifier.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("success", false));
        }
        if (rawBody.length() > 32_768) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }
        if (!signatureVerifier.isValid(rawBody, timestamp, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }
        SePayWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, SePayWebhookPayload.class);
        } catch (JsonProcessingException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }
        try {
            bankPaymentService.processWebhook(payload);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }
    }
}
