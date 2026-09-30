package com.example.webhookserver.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OAuth2-style token endpoint implementing the <b>client-credentials</b> grant.
 * <p>
 * This makes the <em>server</em> the token authority: a client authenticates
 * with its {@code client_id}/{@code client_secret}, and the server mints and
 * signs a short-lived RS256 JWT via {@link JwtUtils} using its RSA private key.
 * The private key never leaves the server — the client only ever receives a
 * finished token, which it then presents to the protected registration endpoint.
 * <p>
 * Request (form-encoded, per RFC 6749):
 * <pre>
 *   POST /oauth/token
 *   grant_type=client_credentials&amp;client_id=...&amp;client_secret=...
 * </pre>
 * Response (JSON):
 * <pre>
 *   { "access_token": "&lt;jwt&gt;", "token_type": "Bearer", "expires_in": 300 }
 * </pre>
 * This endpoint is intentionally <b>not</b> protected by the JWT filter/resource
 * server — it is guarded by the client credential itself, which is how a real
 * token endpoint works.
 */
@RestController
@EnableConfigurationProperties(AuthProperties.class)
public class TokenController {

    private static final Logger log = LoggerFactory.getLogger(TokenController.class);

    private final AuthProperties props;
    private final RsaKeyProvider keys;

    public TokenController(AuthProperties props, RsaKeyProvider keys) {
        this.props = props;
        this.keys = keys;
    }

    @PostMapping(value = "/oauth/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> issueToken(
            @RequestParam(name = "grant_type", required = false) String grantType,
            @RequestParam(name = "client_id", required = false) String clientId,
            @RequestParam(name = "client_secret", required = false) String clientSecret) {

        log.info("token endpoint: token request grant_type='{}' client_id='{}'", grantType, clientId);

        // Only the client-credentials grant is supported here.
        if (!"client_credentials".equals(grantType)) {
            log.warn("token endpoint: REJECTED — unsupported grant_type '{}'", grantType);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "unsupported_grant_type"));
        }

        // Verify the client credential (constant-time to avoid timing leaks).
        boolean idOk = props.getClientId().equals(clientId);
        boolean secretOk = clientSecret != null && constantTimeEquals(props.getClientSecret(), clientSecret);
        if (!idOk || !secretOk) {
            log.warn("token endpoint: REJECTED — invalid client credentials for client_id='{}'", clientId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client"));
        }

        // Mint the JWT on the server, signed with the RSA private key — which
        // never leaves the server.
        String jwt = JwtUtils.mint(
                keys.getPrivateKey(),
                clientId,               // sub = the authenticated client
                props.getIssuer(),
                props.getAudience(),
                props.getTtlSeconds(),
                null);

        log.info("token endpoint: ISSUED token for client_id='{}' (ttl={}s)", clientId, props.getTtlSeconds());
        return ResponseEntity.ok(Map.of(
                "access_token", jwt,
                "token_type", "Bearer",
                "expires_in", props.getTtlSeconds()));
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ab.length != bb.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < ab.length; i++) {
            result |= ab[i] ^ bb[i];
        }
        return result == 0;
    }
}
