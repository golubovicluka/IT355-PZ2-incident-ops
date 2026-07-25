package com.golubovicluka.incident_ops.identity.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
		@NotBlank(message = "Display name is required")
		@Size(
				min = 2,
				max = 150,
				message = "Display name must contain between 2 and 150 characters")
		String displayName,
		@NotBlank(message = "Username is required")
		@Size(
				min = 3,
				max = 100,
				message = "Username must contain between 3 and 100 characters")
		@Pattern(
				regexp = "^[a-zA-Z0-9._-]+$",
				message = "Username may contain only letters, numbers, dots, dashes, and underscores")
		String username,
		@NotBlank(message = "Password is required")
		@Size(
				min = 8,
				max = 200,
				message = "Password must contain between 8 and 200 characters")
		String password) {
}
