package com.example.webhookserver.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads the bundled RSA keypair (PEM) used for RS256 token signing/verification.
 * <p>
 * The <b>private key</b> is used only by the token endpoint to sign tokens; the
 * <b>public key</b> is used to verify them. With asymmetric keys, whoever holds
 * the public key can validate a token but cannot forge one — the private key
 * never needs to be shared with verifiers.
 * <p>
 * <b>PoC note:</b> the private key is bundled in resources purely for a
 * self-contained demo. In a real system the private key would live only in the
 * issuer and be provisioned via a secret manager, never committed to source.
 */
@Component
@EnableConfigurationProperties(AuthProperties.class)
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public RsaKeyProvider(AuthProperties props, ResourceLoader resourceLoader) throws Exception {
        this.privateKey = loadPrivateKey(resourceLoader, props.getPrivateKeyLocation());
        this.publicKey = loadPublicKey(resourceLoader, props.getPublicKeyLocation());
        log.info("Loaded RSA keypair for RS256 (private from '{}', public from '{}')",
                props.getPrivateKeyLocation(), props.getPublicKeyLocation());
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    private static RSAPrivateKey loadPrivateKey(ResourceLoader loader, String location) throws Exception {
        byte[] der = readPemBody(loader, location, "PRIVATE KEY");
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static RSAPublicKey loadPublicKey(ResourceLoader loader, String location) throws Exception {
        byte[] der = readPemBody(loader, location, "PUBLIC KEY");
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(der));
    }

    /** Reads a PEM file, strips the header/footer, and base64-decodes the body to DER. */
    private static byte[] readPemBody(ResourceLoader loader, String location, String kind) throws Exception {
        try (InputStream in = loader.getResource(location).getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem
                    .replace("-----BEGIN " + kind + "-----", "")
                    .replace("-----END " + kind + "-----", "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(base64);
        }
    }
}
