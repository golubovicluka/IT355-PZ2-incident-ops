package com.golubovicluka.incident_ops.identity.web.request;

import com.golubovicluka.incident_ops.identity.domain.Team;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamRequest(
		@NotBlank(message = "Team name is required")
		@Size(
				max = Team.MAX_NAME_LENGTH,
				message = "Team name must not exceed 100 characters")
		String name) {
}
