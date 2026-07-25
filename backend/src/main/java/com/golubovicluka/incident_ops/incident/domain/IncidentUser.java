package com.golubovicluka.incident_ops.incident.domain;

import java.util.Locale;
import java.util.Objects;

public record IncidentUser(Long id, String username, String displayName) {

	public IncidentUser {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException("user id must be positive");
		}
		username = requireText(username, "username").toLowerCase(Locale.ROOT);
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
