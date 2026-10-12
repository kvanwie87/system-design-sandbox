package com.example.demo.jwt;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * RSA key material and JWT encoder/decoder for the "jwt" profile (RS256).
 *
 * <p>The <b>private key signs</b> issued tokens; the <b>public key verifies</b>
 * them — the asymmetric "one signer, many verifiers" model.
 *
 * <p><b>The key pair below is HARDCODED for this learning sandbox only.</b> A
 * fixed key means tokens stay valid across restarts and — importantly — every
 * instance uses the same key, which is what makes verification work when the app
 * is scaled horizontally (a token issued by one instance verifies on any other).
 *
 * <p><b>Do NOT do this in production.</b> A committed private key is a secret
 * leaked into source control. In a real system the key material must come from:
 * <ul>
 *   <li>shared <b>secure storage</b> (a keystore/secret loaded at runtime from a
 *       secrets manager or vault), identical across all instances; or</li>
 *   <li>a dedicated <b>issuer + JWKS</b>: a separate auth server owns the private
 *       key and signs tokens, while these services only verify, fetching the
 *       public keys from the issuer's JWKS endpoint (configured via
 *       {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}). This also
 *       enables central key rotation via the {@code kid} header.</li>
 * </ul>
 *
 * <p>Only active under the {@code jwt} profile, so these beans don't load for
 * the other profiles.
 */
@Configuration
@Profile("jwt")
public class JwtKeyConfig {

	/** Stable key id — fixed so the {@code kid} doesn't change across restarts. */
	private static final String KEY_ID = "spring-security-poc-demo-key";

	// --- HARDCODED DEMO KEY PAIR (sandbox only; never commit real keys) --------

	private static final String PRIVATE_KEY_PEM = """
		MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDSf7jxQlubcuc5
		6qsOLjoBhX7SIoxp6D31Sf34OJWEbr2eFuy8ecgCUklyOtdCFNh3b0U2eFVr7rYM
		sAqNWr9SZEwYrwMLmhE+P3fY8u1BL6aqwIvbNYf8XpSv0seXO0ZY+tlWBiRBpLG0
		soL7dHaJe/nGhn3Cgu1kxq5cbQOt5pOnXqHMy9S8aTeElihIs1xr+82DT+k3sy+F
		bxOxM7I2CkfiXH6upVWVt0twQKqtI0FdS2WFbxhSJKhXP77sxTDmVWtVVTUOd+/z
		gHVeXDZlOutp7d+OSCJOIlttJW56OwHN+Dspp4bT4okFMeGfslrD5tjg9Hvdfkm7
		l0UN6bN5AgMBAAECggEBALCjkzuJFrxccKKORNqSegSa1HBbo36f75YtcBxixz0p
		fJ7z19bVTrx79rwPmmEOgxO+YNMzqWL4HvaIp2I0+4WVOrIf8qtCryOQOO7p86QW
		64IwL2xVAHcLmzPr6acDi6XRYp2oa7ZWEPV0Myv1cB8w1Mn+Q/v8yF8fxmMy67p5
		06Cv1emlcwxu+pRKSlAmxBRvvX66TJwdDe9uuwBlWE7CHR3MJHrj6TbtGbfHuUFA
		MkMz59Ck3xeLivLVw1dxso2X6+z1AcDwjD1jzIBfY3P2jnN8FCpmfiGNvm0PwCrb
		YwKtvvHjH9RF3XCr/P5KJ4JJV2UGzAs+cUcfW3oKDYECgYEA9zhAatHEPMKabYu7
		1n0IN9suyQypdWjAbvxesfQV/jrFH8oMGY9dHS+oj0dtuXCA4cU21/iIDv3GExY9
		CJJ9uXjTYWMF/qMpLfHQMvU0TrahCL9CziCgAnknGXff80DNeS40/f5YKFV4gYJC
		gAFnpD0tYmfoIVSn2adrKQB4tVECgYEA2fmZ51+sTUTNyI5Dkfi5Pd1L+jTW1tYV
		/D4PXqDtt2Nzdv9Cggi3uGR2zss6qNaXMccrTmv75+jcKZeSMj0wqW4aAdfAzfF8
		V9Ybx8eLkZY1g1ePhFazNJHTJ7puiX+Py3zcX3RpTaLlhukqXuJy/37ZVQoFjpwF
		7hYPK+fNsakCgYEA0Kvkg8Wn1HmWjBzEYlmLVEpoBT/w0a+nrobLD34uCjdlKF4N
		NI30VKxjpPzvuvUhdHBYyJd5kN3YEUhWICQ2hxVbpvpElxj1q1hPGpQqoL460v1N
		0FttdGj6/d5XZCYwybac+D91/iWW4AopQ47sAo+4/TUjlO6191v/cc56FOECgYBv
		ILW6JCiv7BgN25nPXcLzK1OE6FNvjA4BX5JuaBVJncZMMK8zpEYmF/IKU8pAMcc7
		dZM6lAsFnM5B1iiOvGuGtb9EmT6OwELodRwMqksZmj8GCyn8Ckeq0X2Zj46zQvmI
		gMvJTowxLI9gTJdxcHm4kG80z7stjZXoYA60D6XqoQKBgCpg2JrjktbA6FJBZ32t
		8GQaHYWmeUTZIw1NWg8XUmM35hyu1w7fIo0NtU1IommlKVJknBnZLgXs981AjTP2
		WNmCKpu42UW9tK/EgMj9yIJ6cAhlkpI4/00Z0AuAfn/aEhPzMonAGNpnVynxEmMH
		jkSpYY7QvSug7nrY6Bc2tSyH
		""";

	private static final String PUBLIC_KEY_PEM = """
		MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0n+48UJbm3LnOeqrDi46
		AYV+0iKMaeg99Un9+DiVhG69nhbsvHnIAlJJcjrXQhTYd29FNnhVa+62DLAKjVq/
		UmRMGK8DC5oRPj932PLtQS+mqsCL2zWH/F6Ur9LHlztGWPrZVgYkQaSxtLKC+3R2
		iXv5xoZ9woLtZMauXG0DreaTp16hzMvUvGk3hJYoSLNca/vNg0/pN7MvhW8TsTOy
		NgpH4lx+rqVVlbdLcECqrSNBXUtlhW8YUiSoVz++7MUw5lVrVVU1Dnfv84B1Xlw2
		ZTrrae3fjkgiTiJbbSVuejsBzfg7KaeG0+KJBTHhn7Jaw+bY4PR73X5Ju5dFDemz
		eQIDAQAB
		""";

	private final RSAKey rsaKey = loadRsaKey();

	/** Encoder used to sign tokens with the RSA private key (RS256). */
	@Bean
	JwtEncoder jwtEncoder() {
		JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(rsaKey));
		return new NimbusJwtEncoder(jwks);
	}

	/** Decoder used to verify incoming tokens with the RSA public key. */
	@Bean
	JwtDecoder jwtDecoder() {
		try {
			return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to build JwtDecoder", ex);
		}
	}

	private static RSAKey loadRsaKey() {
		try {
			KeyFactory factory = KeyFactory.getInstance("RSA");

			byte[] privateDer = Base64.getDecoder().decode(stripPem(PRIVATE_KEY_PEM));
			RSAPrivateKey privateKey = (RSAPrivateKey) factory
				.generatePrivate(new PKCS8EncodedKeySpec(privateDer));

			byte[] publicDer = Base64.getDecoder().decode(stripPem(PUBLIC_KEY_PEM));
			RSAPublicKey publicKey = (RSAPublicKey) factory
				.generatePublic(new X509EncodedKeySpec(publicDer));

			return new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(KEY_ID)
				.build();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to load RSA key", ex);
		}
	}

	/** Remove PEM headers/footers and whitespace, leaving raw Base64. */
	private static String stripPem(String pem) {
		return pem
			.replaceAll("-----BEGIN [^-]+-----", "")
			.replaceAll("-----END [^-]+-----", "")
			.replaceAll("\\s", "");
	}

}
