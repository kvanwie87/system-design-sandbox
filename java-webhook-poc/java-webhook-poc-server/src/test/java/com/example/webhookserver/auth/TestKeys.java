package com.example.webhookserver.auth;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Test helpers for RS256 keys: loads the bundled private key (so tests can mint
 * tokens the server will accept) and generates a throwaway "wrong" key (so tests
 * can prove a token signed by a different key is rejected).
 */
final class TestKeys {

    private TestKeys() {
    }

    /** Loads the bundled RSA private key from the classpath (keys/private_key.pem). */
    static RSAPrivateKey bundledPrivateKey() {
        try (var in = TestKeys.class.getResourceAsStream("/keys/private_key.pem")) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load bundled test private key", e);
        }
    }

    /** Loads the bundled RSA public key from the classpath (keys/public_key.pem). */
    static RSAPublicKey bundledPublicKey() {
        try (var in = TestKeys.class.getResourceAsStream("/keys/public_key.pem")) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load bundled test public key", e);
        }
    }

    /** Generates a fresh, unrelated RSA private key for negative "forgery" tests. */
    static RSAPrivateKey freshWrongPrivateKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            return (RSAPrivateKey) pair.getPrivate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate wrong key", e);
        }
    }
}
