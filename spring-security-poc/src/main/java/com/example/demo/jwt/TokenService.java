package com.example.demo.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Builds and signs JWTs (RS256) from an authenticated principal.
 *
 * <p>The token carries standard claims — issuer, subject (username), issued-at,
 * expiry — plus a {@code roles} claim listing the user's authorities (without
 * the {@code ROLE_} prefix). The resource server later maps that claim back to
 * Spring Security authorities.
 */
@Service
@Profile("jwt")
public class TokenService {

	private static final Duration TTL = Duration.ofMinutes(30);

	private final JwtEncoder jwtEncoder;

	public TokenService(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	public String issueToken(Authentication authentication) {
		Instant now = Instant.now();

		String roles = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)       // e.g. ROLE_USER
			.map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
			.collect(Collectors.joining(" "));

		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("spring-security-poc")
			.issuedAt(now)
			.expiresAt(now.plus(TTL))
			.subject(authentication.getName())
			.claim("roles", roles)
			.build();

		JwsHeader header = JwsHeader.with(() -> "RS256").build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
