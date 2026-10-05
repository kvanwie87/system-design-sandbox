package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.user.AppUser;
import com.example.demo.user.RegistrationRequest;
import com.example.demo.user.UserService;
import com.example.demo.user.UsernameAlreadyExistsException;

import jakarta.validation.Valid;

/**
 * Self-service user registration.
 *
 * <p>{@code POST /register} with a JSON body {@code {"username","password"}}.
 * This endpoint must be publicly accessible so new users can sign up — the
 * security profiles permit it without authentication.
 *
 * <p>The response never includes the password (or its hash).
 */
@RestController
public class RegistrationController {

	private final UserService userService;

	public RegistrationController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Object> register(@Valid @RequestBody RegistrationRequest request) {
		AppUser created = userService.register(request.username(), request.password());
		return Map.of(
			"id", created.getId(),
			"username", created.getUsername(),
			"roles", created.getRoles());
	}

	/** Duplicate username -> 409 Conflict. */
	@ExceptionHandler(UsernameAlreadyExistsException.class)
	public ResponseEntity<Map<String, String>> handleDuplicate(UsernameAlreadyExistsException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(Map.of("error", ex.getMessage()));
	}

}
