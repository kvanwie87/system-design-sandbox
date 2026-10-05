package com.example.demo.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	@GetMapping("/hello")
	public String hello() {
		return "Hello, World!";
	}

	/**
	 * Returns the current CSRF token.
	 *
	 * <p>Spring Security resolves the {@link CsrfToken} from the request and
	 * injects it as a parameter. Accessing it here forces the (lazily loaded)
	 * token to be materialized and persisted, so this endpoint doubles as a way
	 * to "prime" a token before making a state-changing request.
	 *
	 * <p>Only meaningful when CSRF is enabled (e.g. the {@code basicwithcsrf}
	 * profile). When CSRF is disabled, {@code token} will be {@code null}.
	 *
	 * <p>Must be a safe method (GET) so it is not itself CSRF-checked.
	 */
	@GetMapping("/csrf")
	public CsrfToken csrf(CsrfToken token) {
		return token;
	}

	/**
	 * A state-changing endpoint used to demonstrate CSRF protection. Echoes the
	 * request body back. Under a CSRF-enabled profile this POST is rejected with
	 * 403 unless a valid CSRF token accompanies the request.
	 */
	@PostMapping("/echo")
	public String echo(@RequestBody(required = false) String body) {
		return "echo: " + (body == null ? "" : body);
	}

}
