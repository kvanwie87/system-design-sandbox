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
 * Security configuration for the "formlogin" profile.
 *
 * <p>All requests require authentication. Spring Security's default login form
 * is used; log in with the user configured under {@code spring.security.user}
 * in application.yaml.
 *
 * <p>Activate with: {@code --spring.profiles.active=formlogin}
 */
@Configuration
@EnableWebSecurity
@Profile("formlogin")
public class FormLoginSecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/register").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.formLogin(Customizer.withDefaults())
			// Public registration endpoint is exempt from CSRF so API clients
			// can register without first fetching a token.
			.csrf(csrf -> csrf.ignoringRequestMatchers("/register"));
		return http.build();
	}

}
