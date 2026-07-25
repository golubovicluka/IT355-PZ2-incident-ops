package com.golubovicluka.incident_ops.incident.domain;

import java.util.Objects;

public record IncidentManagedService(Long id, String name) {

	public IncidentManagedService {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException("managed service id must be positive");
		}
		name = requireText(name, "managed service name");
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
