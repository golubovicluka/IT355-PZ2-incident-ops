package com.golubovicluka.incident_ops.identity.domain;

import java.util.Objects;

public record Team(Long id, String name) {

	public Team {
		name = requireText(name, "name");
	}

	public static Team create(String name) {
		return new Team(null, name);
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
