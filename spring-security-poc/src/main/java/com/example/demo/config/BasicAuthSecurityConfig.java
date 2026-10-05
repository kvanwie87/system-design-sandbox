package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the "basic" profile.
 *
 * <p>All requests require authentication via HTTP Basic. This is convenient for
 * API/command-line testing (e.g. {@code curl -u user:password}) since there is
 * no login form or session to deal with.
 *
 * <p>CSRF protection is disabled here: HTTP Basic is typically used by
 * non-browser clients that authenticate on every request, so the CSRF token
 * workflow does not apply. (For browser, session-based auth, keep CSRF on.)
 *
 * <p>Activate with: {@code --spring.profiles.active=basic}
 */
@Configuration
@EnableWebSecurity
@Profile("basic")
public class BasicAuthSecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/register").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable());
		return http.build();
	}

}
