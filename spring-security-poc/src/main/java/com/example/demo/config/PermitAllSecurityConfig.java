package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the "permitall" profile.
 *
 * <p>Every request is allowed through without authentication. Useful for
 * comparing against a secured setup and for experimenting freely.
 *
 * <p>Activate with: {@code --spring.profiles.active=permitall}
 */
@Configuration
@EnableWebSecurity
@Profile("permitall")
public class PermitAllSecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.anyRequest().permitAll());
		return http.build();
	}

}
