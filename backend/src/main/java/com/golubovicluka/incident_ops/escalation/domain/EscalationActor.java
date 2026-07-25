package com.golubovicluka.incident_ops.escalation.domain;

import java.util.Objects;

public record EscalationActor(Long id, String username, String displayName) {

	public EscalationActor {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException("actor id must be positive");
		}
		username = requireText(username, "username");
		displayName = requireText(displayName, "displayName");
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field + " must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return normalized;
	}
}
