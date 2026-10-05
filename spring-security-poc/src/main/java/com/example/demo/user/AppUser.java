package com.example.demo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persisted application user. Named {@code AppUser} to avoid confusion with
 * Spring Security's own {@code User} class.
 *
 * <p>{@code roles} is stored as a comma-separated string (e.g. "USER,ADMIN")
 * to keep the schema simple for this sandbox. The password is stored as a
 * BCrypt hash, never plaintext.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	/** BCrypt-hashed password. */
	@Column(nullable = false)
	private String password;

	/** Comma-separated role names without the ROLE_ prefix, e.g. "USER,ADMIN". */
	@Column(nullable = false)
	private String roles;

	private boolean enabled;

}
