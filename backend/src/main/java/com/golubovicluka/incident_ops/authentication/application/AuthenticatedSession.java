package com.golubovicluka.incident_ops.authentication.application;

import java.time.Instant;
import java.util.Set;

public record AuthenticatedSession(
		String token,
		Instant expiresAt,
		String username,
		String displayName,
		Set<String> roles) {

	public AuthenticatedSession {
		roles = Set.copyOf(roles);
	}

	@Override
	public String toString() {
		return "AuthenticatedSession[token=[REDACTED], expiresAt=%s, username=%s, displayName=%s, roles=%s]"
				.formatted(expiresAt, username, displayName, roles);
	}
}
