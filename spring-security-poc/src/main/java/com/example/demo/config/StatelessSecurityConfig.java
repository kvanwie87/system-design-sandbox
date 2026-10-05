package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the "stateless" profile.
 *
 * <p>HTTP Basic authentication with {@link SessionCreationPolicy#STATELESS}, so
 * Spring Security never creates or uses an {@code HttpSession}. Every request
 * must carry its own credentials (the {@code Authorization} header); nothing is
 * remembered between requests and no {@code JSESSIONID} cookie is issued.
 *
 * <p>This mirrors how token-based APIs (e.g. JWT / OAuth2 resource servers) are
 * set up, without actually introducing tokens. CSRF is disabled because there
 * is no session/cookie for a cross-site request to ride on.
 *
 * <p>Contrast with {@code basic}: both use HTTP Basic, but {@code basic} uses
 * the default {@code IF_REQUIRED} session policy (a session may be created),
 * whereas this profile guarantees none is.
 *
 * <p>Activate with: {@code --spring.profiles.active=stateless}
 */
@Configuration
@EnableWebSecurity
@Profile("stateless")
public class StatelessSecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/register").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable());
		return http.build();
	}

}
