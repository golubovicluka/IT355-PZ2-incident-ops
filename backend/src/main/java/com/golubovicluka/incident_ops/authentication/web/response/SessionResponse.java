package com.golubovicluka.incident_ops.authentication.web.response;

import java.time.Instant;
import java.util.Set;

import com.golubovicluka.incident_ops.authentication.application.AuthenticatedSession;

public record SessionResponse(
		String token,
		Instant expiresAt,
		String username,
		String displayName,
		Set<String> roles) {

	public SessionResponse {
		roles = Set.copyOf(roles);
	}

	public static SessionResponse from(AuthenticatedSession session) {
		return new SessionResponse(
				session.token(),
				session.expiresAt(),
				session.username(),
				session.displayName(),
				session.roles());
	}

	@Override
	public String toString() {
		return "SessionResponse[token=[REDACTED], expiresAt=%s, username=%s, displayName=%s, roles=%s]"
				.formatted(expiresAt, username, displayName, roles);
	}
}
