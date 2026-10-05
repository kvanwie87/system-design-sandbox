package com.example.demo.user;

/**
 * Thrown when attempting to register a username that is already taken.
 * Mapped to HTTP 409 Conflict by the controller.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

	public UsernameAlreadyExistsException(String username) {
		super("Username already exists: " + username);
	}

}
