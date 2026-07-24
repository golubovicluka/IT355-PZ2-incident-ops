package com.golubovicluka.incident_ops.identity.domain;

import java.util.Objects;

public record Team(Long id, String name) {

	public static final int MAX_NAME_LENGTH = 100;

	public Team {
		name = requireText(name, "name");
		if (name.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException(
					"name must not exceed " + MAX_NAME_LENGTH + " characters");
		}
	}

	public static Team create(String name) {
		return new Team(null, name);
	}

	public Team rename(String name) {
		return new Team(id, name);
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
