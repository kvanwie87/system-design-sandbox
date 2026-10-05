package com.example.demo.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads users from the database for authentication.
 *
 * <p>Defining this bean causes Spring Boot to back off its default single-user
 * configuration (the {@code spring.security.user.*} properties no longer apply).
 * It is wired into the built-in {@code DaoAuthenticationProvider}, which handles
 * the actual password comparison via the {@code PasswordEncoder}. No custom
 * {@code AuthenticationProvider} is required for database-backed username/password
 * authentication.
 *
 * <p>Because this is a plain shared bean (not tied to a {@code @Profile}), every
 * security profile authenticates against these database users.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public DatabaseUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser appUser = userRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		// roles are stored comma-separated without the ROLE_ prefix, e.g. "USER,ADMIN"
		String[] roles = appUser.getRoles().split(",");

		return User.withUsername(appUser.getUsername())
			.password(appUser.getPassword())   // already a BCrypt hash
			.roles(roles)
			.disabled(!appUser.isEnabled())
			.build();
	}

}
