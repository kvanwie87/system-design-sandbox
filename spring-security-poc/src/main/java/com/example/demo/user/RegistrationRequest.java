package com.example.demo.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /register}. Bean Validation constraints are
 * enforced by {@code @Valid} in the controller; a violation yields a 400.
 */
public record RegistrationRequest(

	@NotBlank(message = "username is required")
	@Size(min = 3, max = 50, message = "username must be 3-50 characters")
	String username,

	@NotBlank(message = "password is required")
	@Size(min = 8, max = 100, message = "password must be 8-100 characters")
	String password

) {
}
