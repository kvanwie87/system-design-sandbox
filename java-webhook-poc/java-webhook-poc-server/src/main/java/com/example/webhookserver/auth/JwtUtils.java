package com.example.webhookserver.auth;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure-JDK RS256 JSON Web Token minting and verification.
 * <p>
 * This intentionally uses <em>only</em> the JDK ({@link java.security.Signature}
 * with {@code SHA256withRSA} and {@link java.util.Base64} for base64url) plus
 * Jackson for JSON — no JWT library. It exists so the "hand-rolled" auth mode can
 * show exactly how an RS256 JWT is built and validated from first principles.
 * <p>
 * The token layout is the standard three base64url segments joined by dots:
 * <pre>
 *   base64url(header) '.' base64url(payload) '.' base64url(RSA-SHA256(signing-input))
 * </pre>
 * where {@code signing-input = base64url(header) '.' base64url(payload)}.
 * <p>
 * With RS256 the keys are asymmetric: the token is signed with the RSA
 * <b>private</b> key (held only by the issuer / token endpoint) and verified with
 * the RSA <b>public</b> key. A verifier holding the public key can validate a
 * token but cannot forge one. The tokens are ordinary, spec-compliant RS256 JWTs,
 * so the Spring Security resource-server mode (a {@code JwtDecoder} configured
 * with the same public key) validates the very same tokens.
 */
public final class JwtUtils {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Fixed header for RS256 tokens: {@code {"alg":"RS256","typ":"JWT"}}. */
    private static final String ENCODED_HEADER =
            URL_ENCODER.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private JwtUtils() {
        // Utility class — no instantiation
    }

    /**
     * Mints an RS256 JWT signed with the given RSA private key.
     *
     * @param privateKey  the RSA private key used to sign
     * @param subject     the {@code sub} claim (who the token represents)
     * @param issuer      the {@code iss} claim
     * @param audience    the {@code aud} claim
     * @param ttlSeconds  seconds until expiry ({@code exp}); {@code iat} is now
     * @param extraClaims optional additional claims (may be null)
     * @return the compact serialized JWT string
     */
    public static String mint(RSAPrivateKey privateKey, String subject, String issuer, String audience,
                              long ttlSeconds, Map<String, Object> extraClaims) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", subject);
        claims.put("iss", issuer);
        claims.put("aud", audience);
        claims.put("iat", now);
        claims.put("exp", now + ttlSeconds);
        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }

        try {
            String encodedPayload = URL_ENCODER.encodeToString(MAPPER.writeValueAsBytes(claims));
            String signingInput = ENCODED_HEADER + "." + encodedPayload;
            String signature = sign(privateKey, signingInput);
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mint JWT", e);
        }
    }

    /**
     * Verifies a token's RSA signature, expiry, issuer and audience, returning the
     * decoded claims on success.
     *
     * @param token            the compact JWT
     * @param publicKey        the RSA public key used to verify the signature
     * @param expectedIssuer   required {@code iss} value
     * @param expectedAudience required {@code aud} value
     * @return the verified claims
     * @throws JwtVerificationException if the token is malformed, incorrectly signed,
     *                                  expired, or has a mismatched issuer/audience
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> verify(String token, RSAPublicKey publicKey,
                                             String expectedIssuer, String expectedAudience) {
        if (token == null || token.isBlank()) {
            throw new JwtVerificationException("Empty token");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtVerificationException("Malformed token: expected 3 segments");
        }
        String signingInput = parts[0] + "." + parts[1];

        // 1. Signature check with the RSA public key
        if (!verifySignature(publicKey, signingInput, parts[2])) {
            throw new JwtVerificationException("Invalid signature");
        }

        // 2. Decode claims
        Map<String, Object> claims;
        try {
            byte[] payloadJson = URL_DECODER.decode(parts[1]);
            claims = MAPPER.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            throw new JwtVerificationException("Unreadable claims payload");
        }

        // 3. Expiry check
        Object exp = claims.get("exp");
        if (!(exp instanceof Number expNum)) {
            throw new JwtVerificationException("Missing exp claim");
        }
        if (Instant.now().getEpochSecond() >= expNum.longValue()) {
            throw new JwtVerificationException("Token expired");
        }

        // 4. Issuer / audience checks
        if (expectedIssuer != null && !expectedIssuer.equals(claims.get("iss"))) {
            throw new JwtVerificationException("Unexpected issuer");
        }
        if (expectedAudience != null && !expectedAudience.equals(claims.get("aud"))) {
            throw new JwtVerificationException("Unexpected audience");
        }

        return claims;
    }

    private static String sign(RSAPrivateKey privateKey, String signingInput) {
        try {
            Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
            signer.initSign(privateKey);
            signer.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private static boolean verifySignature(RSAPublicKey publicKey, String signingInput, String encodedSignature) {
        try {
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(URL_DECODER.decode(encodedSignature));
        } catch (Exception e) {
            // Malformed signature bytes, wrong key, etc. — treat as verification failure.
            return false;
        }
    }

    /** Thrown when a token fails verification for any reason. */
    public static class JwtVerificationException extends RuntimeException {
        public JwtVerificationException(String message) {
            super(message);
        }
    }
}
