package com.example.webhookclient.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Client-side auth configuration bound from {@code webhook.auth.*}.
 * <p>
 * In this client-credentials flow the client does <em>not</em> sign tokens.
 * It authenticates to the server's token endpoint with its
 * {@code client-id}/{@code client-secret} and receives a ready-made JWT, which
 * it then presents to the protected registration endpoint. The JWT signing
 * secret lives only on the server.
 * <p>
 * The {@code mode} mirrors the server's setting:
 * <ul>
 *   <li>{@code spring-security} or {@code hand-rolled} — fetch a token and attach it as a Bearer</li>
 *   <li>{@code none} — send no Authorization header</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "webhook.auth")
public class AuthProperties {

    /** Auth strategy: spring-security | hand-rolled | none. */
    private String mode = "none";

    /** URL of the server's OAuth2 token endpoint (client-credentials grant). */
    private String tokenUrl = "http://localhost:8080/oauth/token";

    /** This client's registered id, presented to the token endpoint. */
    private String clientId = "webhook-client";

    /** This client's secret, presented to the token endpoint. */
    private String clientSecret = "webhook-client-secret-change-me";

    public boolean isEnabled() {
        return !"none".equalsIgnoreCase(mode);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
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
