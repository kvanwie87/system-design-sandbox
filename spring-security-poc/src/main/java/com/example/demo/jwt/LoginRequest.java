package com.example.demo.jwt;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /login}.
 */
public record LoginRequest(

	@NotBlank(message = "username is required")
	String username,

	@NotBlank(message = "password is required")
	String password

) {
}
