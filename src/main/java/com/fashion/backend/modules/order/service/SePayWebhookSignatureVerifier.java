package com.fashion.backend.modules.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class SePayWebhookSignatureVerifier {

    @Value("${app.payment.sepay-webhook-secret:}")
    private String secret;

    @Value("${app.payment.sepay-webhook-tolerance-seconds:300}")
    private long timestampToleranceSeconds;

    public boolean isConfigured() {
        return secret != null && !secret.isBlank();
    }

    public boolean isValid(String rawBody, String timestampHeader, String signatureHeader) {
        if (!isConfigured() || rawBody == null || timestampHeader == null || signatureHeader == null) {
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException ex) {
            return false;
        }
        if (Math.abs(Instant.now().getEpochSecond() - timestamp) > timestampToleranceSeconds) {
            return false;
        }

        String received = signatureHeader.startsWith("sha256=")
                ? signatureHeader.substring("sha256=".length()) : "";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((timestampHeader + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            byte[] actual = HexFormat.of().parseHex(received);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }
}
