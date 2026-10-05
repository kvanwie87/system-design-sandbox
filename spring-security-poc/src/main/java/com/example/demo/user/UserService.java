package com.example.demo.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user accounts, including self-service registration.
 */
@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Registers a new user with role USER. The raw password is hashed with the
	 * {@link PasswordEncoder} before being stored — plaintext passwords are
	 * never persisted.
	 *
	 * @throws UsernameAlreadyExistsException if the username is taken
	 * @return the persisted user
	 */
	@Transactional
	public AppUser register(String username, String rawPassword) {
		if (userRepository.findByUsername(username).isPresent()) {
			throw new UsernameAlreadyExistsException(username);
		}
		AppUser appUser = AppUser.builder()
			.username(username)
			.password(passwordEncoder.encode(rawPassword))
			.roles("USER")
			.enabled(true)
			.build();
		return userRepository.save(appUser);
	}

}
