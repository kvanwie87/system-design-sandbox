package com.example.webhookserver.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the client-credentials token endpoint (POST /oauth/token): valid
 * credentials yield a usable Bearer JWT; bad credentials and unsupported grants
 * are rejected. The issued token is then verified with the bundled RSA public
 * key to confirm it is a well-formed, correctly-signed RS256 JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "webhook.auth.mode=hand-rolled",
        "webhook.auth.issuer=" + TokenEndpointTest.ISSUER,
        "webhook.auth.audience=" + TokenEndpointTest.AUDIENCE,
        "webhook.auth.client-id=" + TokenEndpointTest.CLIENT_ID,
        "webhook.auth.client-secret=" + TokenEndpointTest.CLIENT_SECRET,
        "webhook.event.interval=3600000"
})
class TokenEndpointTest {

    static final String ISSUER = "webhook-auth";
    static final String AUDIENCE = "webhook-server";
    static final String CLIENT_ID = "webhook-client";
    static final String CLIENT_SECRET = "s3cr3t-for-tests";

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> requestToken(String form) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void issuesUsableTokenForValidCredentials() throws Exception {
        HttpResponse<String> response = requestToken(
                "grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("access_token").contains("Bearer").contains("expires_in");

        // Extract the token and confirm it verifies with the bundled public key.
        String token = extractJson(response.body(), "access_token");
        var claims = JwtUtils.verify(token, TestKeys.bundledPublicKey(), ISSUER, AUDIENCE);
        assertThat(claims.get("sub")).isEqualTo(CLIENT_ID);
    }

    @Test
    void rejectsWrongClientSecret() throws Exception {
        HttpResponse<String> response = requestToken(
                "grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=wrong");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("invalid_client");
    }

    @Test
    void rejectsUnknownClientId() throws Exception {
        HttpResponse<String> response = requestToken(
                "grant_type=client_credentials&client_id=nobody&client_secret=" + CLIENT_SECRET);
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void rejectsUnsupportedGrantType() throws Exception {
        HttpResponse<String> response = requestToken(
                "grant_type=password&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("unsupported_grant_type");
    }

    // Minimal JSON string-field extractor for "field":"value".
    private static String extractJson(String json, String field) {
        String needle = "\"" + field + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) {
            throw new AssertionError("field '" + field + "' not found in: " + json);
        }
        start += needle.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
