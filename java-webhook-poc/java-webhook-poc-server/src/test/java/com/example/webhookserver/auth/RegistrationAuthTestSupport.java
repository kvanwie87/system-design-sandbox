package com.example.webhookserver.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared HTTP-level assertions for the protected registration endpoint.
 * <p>
 * Subclasses fix a specific {@code webhook.auth.mode} so the same behavioral
 * contract is verified against both the Spring Security and hand-rolled modes:
 * a valid token is accepted; missing, malformed, tampered, expired, and
 * wrong-audience tokens are rejected with 401.
 * <p>
 * Uses the JDK {@link HttpClient} directly to avoid coupling to Spring Boot's
 * test client packaging (which changed across versions).
 */
abstract class RegistrationAuthTestSupport {

    protected static final String ISSUER = "webhook-auth";
    protected static final String AUDIENCE = "webhook-server";

    private static final String BODY = "{\"callbackUrl\":\"http://localhost:9999/webhook/events\"}";

    // Bundled key => tokens the server accepts; a fresh key => forged tokens it must reject.
    private final java.security.interfaces.RSAPrivateKey goodKey = TestKeys.bundledPrivateKey();
    private final java.security.interfaces.RSAPrivateKey wrongKey = TestKeys.freshWrongPrivateKey();

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private int register(String bearer) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/webhooks/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(BODY));
        if (bearer != null) {
            builder.header("Authorization", bearer);
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    @Test
    void acceptsValidToken() throws Exception {
        String token = JwtUtils.mint(goodKey, "webhook-client", ISSUER, AUDIENCE, 300, null);
        assertThat(register("Bearer " + token)).isEqualTo(200);
    }

    @Test
    void rejectsMissingToken() throws Exception {
        assertThat(register(null)).isEqualTo(401);
    }

    @Test
    void rejectsMalformedHeader() throws Exception {
        assertThat(register("Bearer not-a-jwt")).isEqualTo(401);
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String token = JwtUtils.mint(goodKey, "webhook-client", ISSUER, AUDIENCE, 300, null);
        String tampered = token.substring(0, token.length() - 2) + "xy";
        assertThat(register("Bearer " + tampered)).isEqualTo(401);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = JwtUtils.mint(goodKey, "webhook-client", ISSUER, AUDIENCE, -1, null);
        assertThat(register("Bearer " + token)).isEqualTo(401);
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        String token = JwtUtils.mint(goodKey, "webhook-client", ISSUER, "other-service", 300, null);
        assertThat(register("Bearer " + token)).isEqualTo(401);
    }

    @Test
    void rejectsTokenSignedByWrongKey() throws Exception {
        // A well-formed token with all the right claims, but signed by a different
        // private key. This is the property HS256 could not demonstrate: a verifier
        // holding only the public key cannot be fooled by a token it didn't sign.
        String forged = JwtUtils.mint(wrongKey, "webhook-client", ISSUER, AUDIENCE, 300, null);
        assertThat(register("Bearer " + forged)).isEqualTo(401);
    }
}
