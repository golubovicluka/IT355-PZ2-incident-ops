package com.golubovicluka.incident_ops.servicecatalog.domain;

import java.util.Objects;

public record ManagedService(
		Long id,
		String name,
		String description,
		Criticality criticality,
		OwningTeam owningTeam) {

	public static final int MAX_NAME_LENGTH = 100;
	public static final int MAX_DESCRIPTION_LENGTH = 500;

	public ManagedService {
		name = requireText(name, "name", MAX_NAME_LENGTH);
		description = requireText(description, "description", MAX_DESCRIPTION_LENGTH);
		criticality = Objects.requireNonNull(criticality, "criticality must not be null");
		owningTeam = Objects.requireNonNull(owningTeam, "owningTeam must not be null");
	}

	public static ManagedService create(
			String name,
			String description,
			Criticality criticality,
			OwningTeam owningTeam) {
		return new ManagedService(null, name, description, criticality, owningTeam);
	}

	public ManagedService update(
			String name,
			String description,
			Criticality criticality,
			OwningTeam owningTeam) {
		return new ManagedService(id, name, description, criticality, owningTeam);
	}

	private static String requireText(String value, String field, int maximumLength) {
		Objects.requireNonNull(value, field + " must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		if (normalized.length() > maximumLength) {
			throw new IllegalArgumentException(
					field + " must not exceed " + maximumLength + " characters");
		}
		return normalized;
	}
}
