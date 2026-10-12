package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the "jwt" profile (stateless, RS256 bearer tokens).
 *
 * <p>Flow: {@code POST /login} issues a signed JWT (see {@code AuthController} /
 * {@code TokenService}); clients send it as {@code Authorization: Bearer
 * <token>}; this chain validates it as an OAuth2 resource server using the
 * public key from {@code JwtKeyConfig}.
 *
 * <ul>
 *   <li>Session policy {@code STATELESS} — no {@code HttpSession}; identity
 *       comes entirely from the token on each request.</li>
 *   <li>{@code /login}, {@code /register}, {@code /error} are public; everything
 *       else requires a valid token.</li>
 *   <li>CSRF disabled — tokens travel in the {@code Authorization} header, which
 *       the browser does not attach automatically, so there is no ambient-cookie
 *       CSRF vector.</li>
 *   <li>The token's {@code roles} claim is mapped to {@code ROLE_*}
 *       authorities so the seeded USER/ADMIN roles keep working.</li>
 * </ul>
 *
 * <p>Activate with: {@code --spring.profiles.active=jwt}
 */
@Configuration
@EnableWebSecurity
@Profile("jwt")
public class JwtSecurityConfig {

	/**
	 * Note on token verification: nothing here names a filter explicitly. The
	 * {@code .oauth2ResourceServer(oauth2 -> oauth2.jwt(...))} call below is a DSL
	 * shorthand — Spring Security's {@code OAuth2ResourceServerConfigurer} reacts
	 * to it by adding a {@code BearerTokenAuthenticationFilter} to the chain. That
	 * filter extracts the {@code Authorization: Bearer <token>} header and
	 * delegates verification (signature via the public key, expiry, etc.) to the
	 * {@code JwtDecoder} bean defined in {@code JwtKeyConfig}. The
	 * {@code jwtAuthenticationConverter} then maps the token's claims to
	 * authorities. (Same pattern as {@code .httpBasic()} adding
	 * {@code BasicAuthenticationFilter}, etc. — you declare intent, the DSL
	 * installs the filters.)
	 */
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/login", "/register").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
			.csrf(csrf -> csrf.disable());
		return http.build();
	}

	/**
	 * Maps the custom {@code roles} claim (space-delimited, no prefix) to Spring
	 * Security authorities with the {@code ROLE_} prefix.
	 */
	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("roles");
		authorities.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	/**
	 * Exposes the AuthenticationManager so {@code AuthController} can authenticate
	 * username/password at {@code /login}. It uses the application's
	 * DatabaseUserDetailsService + PasswordEncoder.
	 */
	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

}
