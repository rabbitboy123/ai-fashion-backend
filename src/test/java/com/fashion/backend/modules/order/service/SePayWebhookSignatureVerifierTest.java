package com.fashion.backend.modules.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class SePayWebhookSignatureVerifierTest {

    @Test
    void verifiesCurrentHmacSignatureOverTimestampAndRawBody() throws Exception {
        SePayWebhookSignatureVerifier verifier = verifier("test-secret", 300);
        String rawBody = "{\"id\":123,\"transferType\":\"in\"}";
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        assertTrue(verifier.isValid(rawBody, timestamp, signature("test-secret", timestamp, rawBody)));
        assertFalse(verifier.isValid(rawBody + " ", timestamp, signature("test-secret", timestamp, rawBody)));
    }

    @Test
    void rejectsStaleOrUnconfiguredWebhook() throws Exception {
        SePayWebhookSignatureVerifier verifier = verifier("test-secret", 10);
        String rawBody = "{}";
        String staleTimestamp = Long.toString(Instant.now().minusSeconds(60).getEpochSecond());

        assertFalse(verifier.isValid(rawBody, staleTimestamp, signature("test-secret", staleTimestamp, rawBody)));
        ReflectionTestUtils.setField(verifier, "secret", " ");
        assertFalse(verifier.isValid(rawBody, Long.toString(Instant.now().getEpochSecond()), "sha256=00"));
    }

    private SePayWebhookSignatureVerifier verifier(String secret, long tolerance) {
        SePayWebhookSignatureVerifier verifier = new SePayWebhookSignatureVerifier();
        ReflectionTestUtils.setField(verifier, "secret", secret);
        ReflectionTestUtils.setField(verifier, "timestampToleranceSeconds", tolerance);
        return verifier;
    }

    private String signature(String secret, String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
        return "sha256=" + HexFormat.of().formatHex(digest);
    }
}
