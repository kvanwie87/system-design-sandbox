package com.example.webhookclient.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Obtains and caches the {@code Authorization} header value for outbound calls
 * to the webhook server, according to the configured {@code webhook.auth.mode}.
 * <p>
 * This implements the client side of the <b>client-credentials</b> grant: the
 * client does not sign tokens itself. It calls the server's token endpoint with
 * its {@code client_id}/{@code client_secret} and receives a ready-made JWT,
 * which it caches until shortly before expiry and reuses across requests. When
 * {@code mode=none}, no token is fetched and requests go out unauthenticated.
 */
@Component
@EnableConfigurationProperties(AuthProperties.class)
public class TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    /** Refresh this many seconds before the token actually expires. */
    private static final long EXPIRY_SKEW_SECONDS = 15;

    private final AuthProperties props;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Simple in-memory cache of the current token and when it expires.
    private volatile String cachedToken;
    private volatile Instant cachedExpiry = Instant.EPOCH;

    public TokenProvider(AuthProperties props) {
        this.props = props;
    }

    /**
     * Returns the {@code Bearer <jwt>} header value to attach, fetching a fresh
     * token from the server if needed, or empty if the client is configured for
     * {@code none}.
     */
    public Optional<String> bearerToken() {
        if (!props.isEnabled()) {
            log.info("Auth mode 'none' — sending request WITHOUT an Authorization header");
            return Optional.empty();
        }
        String token = currentToken();
        return token == null ? Optional.empty() : Optional.of("Bearer " + token);
    }

    /** Returns a valid cached token, fetching a new one if missing or near expiry. */
    private synchronized String currentToken() {
        if (cachedToken != null && Instant.now().isBefore(cachedExpiry.minusSeconds(EXPIRY_SKEW_SECONDS))) {
            log.debug("Reusing cached access token (expires {})", cachedExpiry);
            return cachedToken;
        }
        return fetchToken();
    }

    /** Performs the client-credentials token request against the server. */
    private String fetchToken() {
        String form = "grant_type=client_credentials"
                + "&client_id=" + enc(props.getClientId())
                + "&client_secret=" + enc(props.getClientSecret());

        log.info("Requesting access token from {} (client_id='{}', grant=client_credentials)",
                props.getTokenUrl(), props.getClientId());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getTokenUrl()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Token request failed: HTTP {} — {}", response.statusCode(), response.body());
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = mapper.readValue(response.body(), Map.class);
            String accessToken = (String) body.get("access_token");
            long expiresIn = body.get("expires_in") instanceof Number n ? n.longValue() : 0L;

            cachedToken = accessToken;
            cachedExpiry = Instant.now().plusSeconds(expiresIn);
            log.info("Received access token (token_type='{}', expires_in={}s)",
                    body.get("token_type"), expiresIn);
            log.debug("Access token ({} chars): {}",
                    accessToken == null ? 0 : accessToken.length(), accessToken);
            return accessToken;
        } catch (Exception e) {
            log.error("Failed to obtain access token from {}: {}", props.getTokenUrl(), e.getMessage());
            return null;
        }
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
