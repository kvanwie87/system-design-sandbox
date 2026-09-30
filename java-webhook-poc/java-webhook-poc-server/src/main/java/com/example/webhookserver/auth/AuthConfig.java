package com.example.webhookserver.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Wires the registration endpoint's protection based on {@code webhook.auth.mode}.
 * <p>
 * Exactly one {@link SecurityFilterChain} is activated at startup:
 * <ul>
 *   <li><b>spring-security</b> — configures this app as an OAuth2 resource server.
 *       A {@link JwtDecoder} backed by the RSA <b>public</b> key validates the
 *       {@code Authorization: Bearer <jwt>} header on the registration endpoint.</li>
 *   <li><b>hand-rolled</b> — Spring Security permits all requests, and the
 *       {@link HandRolledJwtFilter} performs pure-JDK RS256 validation instead.</li>
 *   <li><b>none</b> — Spring Security permits all requests; no token required.</li>
 * </ul>
 * Both authenticated modes verify RS256 tokens with the same public key
 * ({@link RsaKeyProvider}), so tokens are interchangeable across modes.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthConfig.class);

    // ---------------------------------------------------------------------
    // Mode: spring-security  (OAuth2 resource server)
    // ---------------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(name = "webhook.auth.mode", havingValue = "spring-security")
    public SecurityFilterChain springSecurityModeChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        log.info("Auth mode: spring-security (OAuth2 resource server, RS256)");
        log.info("spring-security auth: protecting POST /api/webhooks/register — Bearer JWT required");

        // Logs the reason for every 401 so rejections are visible in the console.
        AuthenticationEntryPoint loggingEntryPoint = (req, resp, ex) -> {
            log.warn("spring-security auth: REJECTED {} {} with 401 — {}",
                    req.getMethod(), req.getServletPath(), ex.getMessage());
            resp.setStatus(401);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"unauthorized\",\"reason\":\"" + ex.getMessage() + "\"}");
        };

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/webhooks/register").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(loggingEntryPoint)
                        .jwt(jwt -> jwt.decoder(jwtDecoder)));
        return http.build();
    }

    /**
     * A {@link JwtDecoder} that verifies the same RS256 tokens minted by
     * {@link JwtUtils}, using the RSA public key plus issuer/audience validation.
     */
    @Bean
    @ConditionalOnProperty(name = "webhook.auth.mode", havingValue = "spring-security")
    public JwtDecoder jwtDecoder(AuthProperties props, RsaKeyProvider keys) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.getPublicKey())
                .signatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(props.getIssuer());
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(props.getAudience()));
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                withIssuer, audienceValidator));

        // Wrap the decoder so we log each decode attempt and its outcome. Spring's
        // resource-server filter otherwise validates silently; this makes the
        // spring-security mode as observable as the hand-rolled one.
        return token -> {
            log.debug("spring-security auth: decoding RS256 token ({} chars, {} segments)",
                    token == null ? 0 : token.length(),
                    token == null ? 0 : token.split("\\.").length);
            try {
                Jwt jwt = decoder.decode(token);
                // Use getClaimAsString for 'iss' — getIssuer() coerces to a URL and
                // would throw for our non-URL issuer value ("webhook-auth").
                log.info("spring-security auth: ACCEPTED token for sub='{}' (iss='{}', aud={}, exp={})",
                        jwt.getSubject(), jwt.getClaimAsString("iss"), jwt.getAudience(), jwt.getExpiresAt());
                return jwt;
            } catch (JwtException e) {
                // Reason is surfaced to the client via the logging entry point.
                log.debug("spring-security auth: token decode failed — {}", e.getMessage());
                throw e;
            }
        };
    }

    // ---------------------------------------------------------------------
    // Mode: hand-rolled  (pure-JDK filter does the work)
    // ---------------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(name = "webhook.auth.mode", havingValue = "hand-rolled")
    public SecurityFilterChain handRolledPermitAllChain(HttpSecurity http) throws Exception {
        log.info("Auth mode: hand-rolled (pure-JDK JWT filter, RS256)");
        // Spring Security steps aside; HandRolledJwtFilter enforces auth.
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "webhook.auth.mode", havingValue = "hand-rolled")
    public HandRolledJwtFilter handRolledJwtFilter(AuthProperties props, RsaKeyProvider keys) {
        return new HandRolledJwtFilter(props, keys);
    }

    // ---------------------------------------------------------------------
    // Mode: none  (default — endpoint open)
    // ---------------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(name = "webhook.auth.mode", havingValue = "none", matchIfMissing = true)
    public SecurityFilterChain noAuthChain(HttpSecurity http) throws Exception {
        log.info("Auth mode: none (registration endpoint is unprotected)");
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
