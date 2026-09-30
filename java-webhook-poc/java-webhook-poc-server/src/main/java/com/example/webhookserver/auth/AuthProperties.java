package com.example.webhookserver.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for how the registration endpoint is protected.
 * <p>
 * Bound from the {@code webhook.auth.*} properties. The {@code mode} property
 * selects which security strategy is wired in at startup:
 * <ul>
 *   <li>{@code spring-security} — Spring Security OAuth2 resource server validates the JWT</li>
 *   <li>{@code hand-rolled} — a plain servlet filter validates the JWT with pure-JDK code</li>
 *   <li>{@code none} — no authentication (default legacy behavior)</li>
 * </ul>
 * Tokens are RS256-signed: the server holds the RSA private key (used by the
 * token endpoint to sign) and the public key (used to verify). The {@code issuer}
 * and {@code audience} are validated in both modes.
 */
@ConfigurationProperties(prefix = "webhook.auth")
public class AuthProperties {

    /** Auth strategy: spring-security | hand-rolled | none. */
    private String mode = "none";

    /** Resource location of the PKCS#8 RSA private key (PEM) used to sign tokens. */
    private String privateKeyLocation = "classpath:keys/private_key.pem";

    /** Resource location of the X.509 RSA public key (PEM) used to verify tokens. */
    private String publicKeyLocation = "classpath:keys/public_key.pem";

    /** Required token issuer (iss claim). */
    private String issuer = "webhook-auth";

    /** Required token audience (aud claim). */
    private String audience = "webhook-server";

    /** Token lifetime in seconds for tokens issued by the token endpoint. */
    private long ttlSeconds = 300;

    /** Registered client credentials the token endpoint accepts (client-credentials grant). */
    private String clientId = "webhook-client";

    /** Secret the client must present to obtain a token. Distinct from the JWT signing secret. */
    private String clientSecret = "webhook-client-secret-change-me";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    public void setPrivateKeyLocation(String privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
    }

    public String getPublicKeyLocation() {
        return publicKeyLocation;
    }

    public void setPublicKeyLocation(String publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
