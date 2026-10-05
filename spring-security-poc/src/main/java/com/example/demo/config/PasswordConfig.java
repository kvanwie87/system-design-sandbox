package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Shared password encoder bean.
 *
 * <p>Not tied to any {@code @Profile}, so it is always available. The
 * {@code DaoAuthenticationProvider} uses it to verify submitted passwords
 * against the BCrypt hashes stored in the database, and the data seeder uses it
 * to hash passwords before saving.
 */
@Configuration
public class PasswordConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
