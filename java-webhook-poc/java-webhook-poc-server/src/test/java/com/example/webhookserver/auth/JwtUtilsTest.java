package com.example.webhookserver.auth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure-JDK RS256 {@link JwtUtils} mint/verify round trip.
 */
class JwtUtilsTest {

    private static final String ISSUER = "webhook-auth";
    private static final String AUDIENCE = "webhook-server";

    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;
    private static RSAPrivateKey wrongPrivateKey;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        privateKey = (RSAPrivateKey) pair.getPrivate();
        publicKey = (RSAPublicKey) pair.getPublic();
        wrongPrivateKey = (RSAPrivateKey) gen.generateKeyPair().getPrivate();
    }

    @Test
    void mintThenVerifyRoundTrips() {
        String token = JwtUtils.mint(privateKey, "client-1", ISSUER, AUDIENCE, 300, null);
        Map<String, Object> claims = JwtUtils.verify(token, publicKey, ISSUER, AUDIENCE);

        assertEquals("client-1", claims.get("sub"));
        assertEquals(ISSUER, claims.get("iss"));
        assertEquals(AUDIENCE, claims.get("aud"));
        assertTrue(claims.containsKey("iat"));
        assertTrue(claims.containsKey("exp"));
    }

    @Test
    void mintedTokenHasThreeSegments() {
        String token = JwtUtils.mint(privateKey, "client-1", ISSUER, AUDIENCE, 300, null);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void rejectsTamperedSignature() {
        String token = JwtUtils.mint(privateKey, "client-1", ISSUER, AUDIENCE, 300, null);
        String tampered = token.substring(0, token.length() - 2) + "xy";
        assertThrows(JwtUtils.JwtVerificationException.class,
                () -> JwtUtils.verify(tampered, publicKey, ISSUER, AUDIENCE));
    }

    @Test
    void rejectsTokenSignedByWrongKey() {
        // Signed by a different private key — verification with our public key must fail.
        String token = JwtUtils.mint(wrongPrivateKey, "client-1", ISSUER, AUDIENCE, 300, null);
        assertThrows(JwtUtils.JwtVerificationException.class,
                () -> JwtUtils.verify(token, publicKey, ISSUER, AUDIENCE));
    }

    @Test
    void rejectsExpiredToken() {
        // ttl of -1 second => exp is already in the past
        String token = JwtUtils.mint(privateKey, "client-1", ISSUER, AUDIENCE, -1, null);
        JwtUtils.JwtVerificationException ex = assertThrows(JwtUtils.JwtVerificationException.class,
                () -> JwtUtils.verify(token, publicKey, ISSUER, AUDIENCE));
        assertTrue(ex.getMessage().toLowerCase().contains("expired"));
    }

    @Test
    void rejectsWrongIssuer() {
        String token = JwtUtils.mint(privateKey, "client-1", "someone-else", AUDIENCE, 300, null);
        assertThrows(JwtUtils.JwtVerificationException.class,
                () -> JwtUtils.verify(token, publicKey, ISSUER, AUDIENCE));
    }

    @Test
    void rejectsWrongAudience() {
        String token = JwtUtils.mint(privateKey, "client-1", ISSUER, "some-other-service", 300, null);
        assertThrows(JwtUtils.JwtVerificationException.class,
                () -> JwtUtils.verify(token, publicKey, ISSUER, AUDIENCE));
    }

    @Test
    void rejectsMalformedToken() {
        assertThrows(JwtUtils.JwtVerificationException.class,
                () -> JwtUtils.verify("not-a-jwt", publicKey, ISSUER, AUDIENCE));
    }
}
