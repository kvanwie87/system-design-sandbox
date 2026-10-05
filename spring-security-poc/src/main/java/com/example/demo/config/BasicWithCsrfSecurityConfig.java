package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Security configuration for the "basicwithcsrf" profile.
 *
 * <p>Like the {@code basic} profile (HTTP Basic auth, all requests
 * authenticated) but with <strong>CSRF protection left ON</strong>. This exists
 * to demonstrate the difference CSRF makes: state-changing requests
 * (POST/PUT/DELETE) are rejected with 403 unless they carry a valid CSRF token,
 * while safe methods (GET) are unaffected.
 *
 * <p>The token is stored via {@link CookieCsrfTokenRepository} (readable by
 * clients, i.e. {@code httpOnly=false}) so a non-browser client can read it from
 * the {@code XSRF-TOKEN} cookie and send it back in the {@code X-XSRF-TOKEN}
 * header. See {@code GET /csrf} for an endpoint that returns the token directly.
 *
 * <p>Activate with: {@code --spring.profiles.active=basicwithcsrf}
 */
@Configuration
@EnableWebSecurity
@Profile("basicwithcsrf")
public class BasicWithCsrfSecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/register").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults())
			.csrf(csrf -> csrf
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				// Public registration endpoint is exempt from CSRF.
				.ignoringRequestMatchers("/register"));
		return http.build();
	}

}
