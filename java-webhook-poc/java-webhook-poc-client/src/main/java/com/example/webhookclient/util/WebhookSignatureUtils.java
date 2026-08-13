package com.example.webhookclient.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Utility class for webhook HMAC-SHA256 signature computation and verification.
 * <p>
 * The signature format is: "sha256=" followed by the hex-encoded HMAC digest.
 * This matches the format used by popular webhook providers (e.g., GitHub, Stripe).
 * <p>
 * Verification uses constant-time comparison ({@link MessageDigest#isEqual})
 * to prevent timing attacks.
 */
public final class WebhookSignatureUtils {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** HTTP header name used to transmit the webhook signature. */
    public static final String SIGNATURE_HEADER = "X-Webhook-Signature";

    private WebhookSignatureUtils() {
        // Utility class — no instantiation
    }

    /**
     * Computes HMAC-SHA256 signature for the given payload using the provided secret.
     *
     * @param secret  the shared secret key
     * @param payload the request body to sign
     * @return hex-encoded HMAC-SHA256 signature prefixed with "sha256="
     */
    public static String computeSignature(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute webhook signature", e);
        }
    }

    /**
     * Verifies a received signature against the expected signature.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param secret            the shared secret key
     * @param payload           the raw request body
     * @param receivedSignature the signature from the X-Webhook-Signature header
     * @return true if the signature is valid, false otherwise
     */
    public static boolean verifySignature(String secret, String payload, String receivedSignature) {
        String expectedSignature = computeSignature(secret, payload);
        // Constant-time comparison prevents timing side-channel attacks
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                receivedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
