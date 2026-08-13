package com.example.webhookserver.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Utility class for computing HMAC-SHA256 webhook signatures.
 * <p>
 * The signature format is: "sha256=" followed by the hex-encoded HMAC digest.
 * This matches the format used by popular webhook providers (e.g., GitHub, Stripe).
 * <p>
 * The server uses this to sign outgoing payloads. Clients independently verify
 * the signature using the same shared secret to ensure authenticity.
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
}
