package com.example.demo.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the in-memory H2 database with demo users on startup.
 *
 * <p>Because H2 is in-memory, the schema and data are recreated on every run, so
 * this inserts the users each time (guarded by an existence check in case the
 * runner is invoked against a persistent DB).
 *
 * <p>Credentials (kept matching the previous {@code spring.security.user}
 * values so existing validation scripts keep working):
 * <ul>
 *   <li>{@code user} / {@code password} — role USER</li>
 *   <li>{@code admin} / {@code admin} — roles ADMIN, USER</li>
 * </ul>
 */
@Component
public class UserDataSeeder implements CommandLineRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		seed("user", "password", "USER");
		seed("admin", "admin", "ADMIN,USER");
	}

	private void seed(String username, String rawPassword, String roles) {
		if (userRepository.findByUsername(username).isPresent()) {
			return;
		}
		AppUser appUser = AppUser.builder()
			.username(username)
			.password(passwordEncoder.encode(rawPassword))
			.roles(roles)
			.enabled(true)
			.build();
		userRepository.save(appUser);
	}

}
