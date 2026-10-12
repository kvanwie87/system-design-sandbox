package com.example.demo.controller;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.jwt.LoginRequest;
import com.example.demo.jwt.TokenService;

import jakarta.validation.Valid;

/**
 * Issues JWTs for the "jwt" profile.
 *
 * <p>{@code POST /login} with {@code {"username","password"}} authenticates the
 * credentials against the configured {@code AuthenticationManager} (which uses
 * the database-backed {@code UserDetailsService}) and, on success, returns a
 * signed RS256 token. Clients then send it as {@code Authorization: Bearer
 * <token>} on subsequent requests.
 *
 * <p>Only active under the {@code jwt} profile.
 */
@RestController
@Profile("jwt")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final TokenService tokenService;

	public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
		this.authenticationManager = authenticationManager;
		this.tokenService = tokenService;
	}

	@PostMapping("/login")
	public Map<String, String> login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		String token = tokenService.issueToken(authentication);
		return Map.of("token", token, "tokenType", "Bearer");
	}

	/** Invalid credentials -> 401 Unauthorized (don't leak which part was wrong). */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(Map.of("error", "Invalid username or password"));
	}

}
