package com.example.webhookserver.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Hand-rolled JWT authentication filter (pure-JDK verification via {@link JwtUtils}).
 * <p>
 * Active only when {@code webhook.auth.mode=hand-rolled}. It guards the
 * registration endpoint by extracting the bearer token from the
 * {@code Authorization} header, verifying it, and returning
 * {@code 401 Unauthorized} on any failure. Non-protected paths pass straight
 * through.
 * <p>
 * Verification uses the RSA public key (via {@link JwtUtils}); the private key
 * that signs tokens lives only in the token endpoint, so this filter can
 * validate but never mint.
 */
public class HandRolledJwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HandRolledJwtFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PROTECTED_PATH = "/api/webhooks/register";

    private final AuthProperties props;
    private final RsaKeyProvider keys;

    public HandRolledJwtFilter(AuthProperties props, RsaKeyProvider keys) {
        this.props = props;
        this.keys = keys;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only guard the registration endpoint; everything else is unprotected.
        return !PROTECTED_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getServletPath();
        log.info("hand-rolled auth: authenticating {} {}", method, path);

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            log.debug("hand-rolled auth: Authorization header {}",
                    header == null ? "is absent" : "does not start with 'Bearer '");
            reject(response, "Missing or malformed Authorization header");
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        log.debug("hand-rolled auth: verifying RS256 token ({} chars, {} segments)",
                token.length(), token.split("\\.").length);
        try {
            Map<String, Object> claims =
                    JwtUtils.verify(token, keys.getPublicKey(), props.getIssuer(), props.getAudience());
            log.info("hand-rolled auth: ACCEPTED token for sub='{}' (iss='{}', aud='{}', exp={})",
                    claims.get("sub"), claims.get("iss"), claims.get("aud"), claims.get("exp"));
        } catch (JwtUtils.JwtVerificationException e) {
            reject(response, e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String reason) throws IOException {
        log.warn("hand-rolled auth: REJECTED registration request with 401 — {}", reason);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"unauthorized\",\"reason\":\"" + reason + "\"}");
    }
}
